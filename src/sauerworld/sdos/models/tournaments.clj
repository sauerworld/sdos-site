(ns sauerworld.sdos.models.tournaments
  (:require [clj-time.core :refer (now)]
            [clj-time.coerce :as tc]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.models.users :as users]
            [sauerworld.sdos.system.database :as db]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc]))

(def ^{:private true} tournament-key-spec
  "Spec to convert Tournament keys to db row."
  {:registration-open? :registration-open})

(def ^{:private true} tournament->db-val-spec
  "Spec to convert Tournament vals to db row."
  {:start-date tc/to-date
   :end-date tc/to-date})

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

(comment
  (defn nested-find-registrations
    [db event-or-events & subrecords]
    {:pre [(coll? event-or-events)]}
    (let [registrations (find-registrations-by-events db event-or-events)]
      (if (contains? (set subrecords) :user)
        (let [users-by-id (->> (set (map :user-id registrations))
                               (users/get-by-id db)
                               (map (fn [u] [(:id u) u]))
                               (into {}))]
          (map (fn [r] (assoc r :user (get users-by-id (:user-id r))))
               registrations))
        registrations)))

  (defn nested-find-events
    [db tournament-or-tournaments & subrecords]
    {:pre [(coll? tournament-or-tournaments)]}
    (let [events (find-events-by-tournaments db tournament-or-tournaments)]
      (if (contains? (set subrecords) :registration)
        (let [registrations-by-event-id (->> (apply nested-find-registrations
                                                    db events subrecords)
                                             (group-by :event-id))]
          (map (fn [e] (assoc e :registration (get registrations-by-event-id (:id e))))
               events))
        events)))

  (defn nested-find-tournaments
    [db tournaments-or-ids & subrecords]
    {:pre [(or (coll? tournaments-or-ids) (integer? tournaments-or-ids))]}
    (let [tournaments (cond
                       (integer? tournaments-or-ids)
                       [(model/read (tournament tournaments-or-ids) db)]
                       (map? tournaments-or-ids) [tournaments-or-ids]
                       (map? (first (tournaments-or-ids))) tournaments-or-ids
                       :else (find-tournaments-by-ids db tournaments-or-ids))]
      (if (contains? (set subrecords) :event)
        (let [events-by-tourney-id (->> (apply nested-find-events
                                               db tournaments subrecords)
                                        (group-by :tournament-id))]
          (map (fn [t] (assoc t :event (get events-by-tourney-id (:id t))))
               tournaments))
        tournaments))))
