(ns sauerworld.sdos.models.tournaments
  (:require [clj-time.core :refer (now)]
            [clj-time.coerce :as tc]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.models.users :as users]
            [sauerworld.sdos.models.events :as events]
            [sauerworld.sdos.models.registrations :as registrations]
            [sauerworld.sdos.system.database :as db]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc]))

(def ^{:private true} tournament-key-spec
  "Spec to convert Tournament keys to db row."
  {:registration-open? :registration-open})

(def ^{:private true} tournament->db-val-spec
  "Spec to convert Tournament vals to db row."
  {:start-date tc/to-timestamp
   :end-date tc/to-timestamp})

(def ^{:private true} ->tournament-val-spec
  {:start-date tc/to-date-time
   :end-date tc/to-date-time})

(def ^{:private true} select-base
  {:select [:*]
   :from [:tournaments]})

(defn db->tournament
  [result]
  (model/->record result tournament-key-spec ->tournament-val-spec true))

(defn tournament->db
  [tourney]
  (model/->db tourney tournament-key-spec tournament->db-val-spec))

(defn create
  [db tourney]
  (->> {:insert-into :tournaments
        :values (tournament->db tourney)}
       sql/format
       (db/write db)))

(defn find-by-id
  [db id]
  (some-> (assoc select-base :where [[:= :id id]])
          (sql/format)
          (db/read db)
          first
          (db->tournament)
          (map db->tournament)))

(defn find-by-ids
  [db ids]
  (some->> (assoc select-base :where [[:in :id ids]])
           (sql/format)
           (db/read db)
           (map db->tournament)))

(defn find-next
  [db & [time]]
  (let [time (or (some-> time tc/to-date)
                 (java.util.Date.))]
    (-> select-base
        (assoc :where [:> :start_date time]
               :order-by [[:start_date :desc]]
               :limit 1)
        (sql/format)
        (->> (db/read db))
        first
        db->tournament)))

(defn find-all
  [db]
  (some->> select-base
           sql/format
           (db/read db)
           (map db->tournament)))

(defn update
  [db tourney]
  (db/write db
            (sql/format
             {:update :tournaments
              :set (-> tourney
                       tournament->db
                       (dissoc :id))
              :where [:= :id (:id tourney)]})))

(defn delete
  [db tourney-or-id]
  (let [id (if (integer? tourney-or-id)
             tourney-or-id
             (:id tourney-or-id))]
    (db/write db
              (sql/format
               {:delete-from :tournaments
                :where [:= :id id]}))))



;;;
;;; Higher-level functions, creating nested relations
;;;

(defn add-registrations-to-event
  ([event registrations]
     (->> registrations
          (filter #(= (:id event) (:event-id %)) registrations)
          (assoc event :registrations)))
  ([event registrations users]
     (let [users-by-id (if (map? users)
                         users
                         (group-by :id users))
           maybe-add-user (fn [r] (if-let [u (get users-by-id (:user-id r))]
                                    (assoc r :user u)
                                    r))]
       (cond-> (add-registrations-to-event event registrations)
               (seq users)
               (update-in [:registrations] (partial map maybe-add-user))))))

(defn combine-tournament-entities
  "Takes a tournament map, and optional collections of events, registrations,
   and users, and combined into a single nested tournament map."
  [{:keys [tournament events registrations users]}]
  (cond-> tournament
          events
          (assoc :events (filter #(= (:id tournament) (:tournament-id %))))
          registrations
          (update-in :events (partial map #(add-registrations-to-event % registrations users)))))


(defn get-tournament-with
  [db id & with]
  (let [relateds (into #{} with)
        tourney (find-by-id db id)
        events (when (and tourney (contains? relateds :events))
                 (events/find-by-tournaments db tourney))
        registrations (when (and events (contains? relateds :registrations))
                        (registrations/find-by-events events))
        users (when (and registrations (contains? relateds :users))
                (users/find-ids db (map :user-id registrations)))]
    (combine-tournament-entities {:tournament tourney
                                  :events events
                                  :registrations registrations
                                  :users users})))
