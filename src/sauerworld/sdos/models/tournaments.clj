(ns sauerworld.sdos.models.tournaments
  (:require [clj-time.core :refer (now)]
            [clj-time.coerce :as tc]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.models.users :as users]
            [sqlingvo.core :refer (sql) :as sql]
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

(defrecord Tournament
    [id name start-date end-date registration-open?]

  model/DatabaseRecord
  (create [this db]
    (jdbc/execute! db
                   (sql
                    (sql/insert :tournaments []
                      (sql/values
                       (model/->db this
                                   tournament-key-spec
                                   tournament->db-val-spec))))))
  (read [this db]
    {:pre [(or id (:date this))]}
    ;; uses id or date (queries between start-date and end-date
    (let [query (sql
                 (sql/select [*]
                   (sql/from :tournaments)
                   (sql/limit 1)
                   (sql/where
                    (cond
                     id '(= :id id)
                     (:date this) '(> :end_date (:date this) :start_date)))))]
      (some-> (jdbc/execute! db query)
              first
              (model/->record tournament-key-spec ->tournament-val-spec true)
              (->> (merge this))
              (dissoc :date))))
  (update [this db]
      {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/update :tournaments
                        (-> this
                            (model/->db tournament-key-spec
                                        tournament->db-val-spec)
                            (dissoc :id))
                      (sql/where '(= :id id))))))
  (delete [this db]
    {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/delete :tournaments
                      (sql/where '(= :id id)))))))

(defn tournament
  "Creates a Tournament record, either from a map or an id."
  [tournament-or-id]
  {:pre [(or (and (integer? tournament-or-id)
                  (pos? tournament-or-id))
             (map? tournament-or-id))]}
  (map->Tournament (if (integer? tournament-or-id)
                     {:id tournament-or-id}
                     tournament-or-id)))

(defn db->tournament
  [result]
  (map->Tournament (model/->record result tournament-key-spec
                                   ->tournament-val-spec true)))

(defn find-tournaments-by-ids
  [db ids]
  (some->> (sql
            (sql/select [*]
              (sql/from :tournaments)
              (sql/where (list :in :id (seq ids)))))
           (jdbc/execute! db)
           (map db->tournament)))

(def ^{:private true} event-key-spec
  {:team-mode? :team-mode})

(def ^{:private true} event->db-val-spec
  {})

(def ^{:private true} ->event-val-spec
  {})

(defrecord Event
    [id tournament-id name mode team-mode?]

  model/DatabaseRecord
  (create [this db]
    {:pre [(integer? id) (integer? tournament-id)]}
    (jdbc/execute! db
                   (sql
                    (sql/insert :events []
                      (sql/values
                       (model/->db this
                                   event-key-spec
                                   event->db-val-spec))))))
  (read [this db]
    {:pre [id]}
    (let [query (sql
                 (sql/select [*]
                   (sql/from :events)
                   (sql/limit 1)
                   (sql/where '(= :id id))))]
      (some-> (jdbc/execute! db query)
              first
              (model/->record event-key-spec ->event-val-spec true)
              (->> (merge this)))))
  (update [this db]
      {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/update :events
                        (-> this
                            (model/->db event-key-spec
                                        event->db-val-spec)
                            (dissoc :id))
                      (sql/where '(= :id id))))))
  (delete [this db]
    {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/delete :events
                      (sql/where '(= :id id)))))))

(defn event
  "Creates an Event record, either from a map or an id."
  [event-or-id]
  {:pre [(or (and (integer? event-or-id)
                  (pos? event-or-id))
             (map? event-or-id))]}
  (map->Event (if (integer? event-or-id)
                {:id event-or-id}
                event-or-id)))

(defn db->event
  [result]
  (map->Event (model/->record result event-key-spec
                              ->event-val-spec true)))

(defn find-events-by-ids
  [db ids]
  (some->> (sql
            (sql/select [*]
              (sql/from :events)
              (sql/where (list :in :id (seq ids)))))
           (jdbc/execute! db)
           (map db->event)))

(defn find-events-by-tournaments
  [db tournaments-or-ids]
  {:pre [(or (integer? tournaments-or-ids) (coll? tournaments-or-ids))]}
  (let [tournament-ids (cond
                        (integer? tournaments-or-ids)
                        [tournaments-or-ids]
                        (map? tournaments-or-ids)
                        [(:id tournaments-or-ids)]
                        (map? (first tournaments-or-ids))
                        (map :id tournaments-or-ids)
                        :else tournaments-or-ids)]
    (some->> (sql
              (sql/select [*]
                (sql/from :events)
                (sql/where (list :in :tournament_id (seq tournament-ids)))))
             (jdbc/execute! db)
             (map db->event))))

(def ^{:private true} registration-key-spec
  {})

(def ^{:private true} registration->db-val-spec
  {:date tc/to-date})

(def ^{:private true} ->registration-val-spec
  {:date tc/to-date-time})

(defrecord Registration
    [id event-id user-id team date]

  model/DatabaseRecord
  (create [this db]
    {:pre [(integer? id) (integer? event-id) (integer? user-id)]}
    (jdbc/execute! db
                   (sql
                    (sql/insert :registrations []
                      (sql/values
                       (model/->db this
                                   registration-key-spec
                                   registration->db-val-spec))))))
  (read [this db]
    {:pre [(or id (and event-id user-id))]}
    (let [query (sql
                 (cond->
                  (sql/select [*]
                    (sql/from :registrations)
                    (sql/limit 1))
                  id
                  (sql/compose
                   (sql/where '(= :id id)))
                  (not id)
                  (sql/compose
                   (sql/where '(= :event_id event-id))
                   (sql/where '(= :user_id user-id) :and))))]
      (some-> (jdbc/execute! db query)
              first
              (model/->record registration-key-spec ->registration-val-spec true)
              (->> (merge this)))))
  (update [this db]
      {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/update :registrations
                        (-> this
                            (model/->db registration-key-spec
                                        registration->db-val-spec)
                            (dissoc :id))
                      (sql/where '(= :id id))))))
  (delete [this db]
    {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/delete :registrations
                      (sql/where '(= :id id)))))))

(defn registration
  "Creates a Registration record, either from a map or an id."
  [registration-or-id]
  {:pre [(or (and (integer? registration-or-id)
                  (pos? registration-or-id))
             (map? registration-or-id))]}
  (map->Registration (if (integer? registration-or-id)
                       {:id registration-or-id}
                       registration-or-id)))

(defn db->registration
  [result]
  (map->Registration (model/->record result registration-key-spec
                                     ->registration-val-spec true)))

(defn find-registrations-by-ids
  [db ids]
  (some->> (sql
            (sql/select [*]
              (sql/from :registrations)
              (sql/where (list :in :id (seq ids)))))
           (jdbc/execute! db)
           (map db->registration)))

(defn find-registrations-by-events
  "Pass a db and any of:
     Event id
     Event
     collection of Events
     collection of Event ids"
  [db events-or-ids]
  {:pre [(or (coll? events-or-ids) (integer? events-or-ids))]}
  (let [event-ids (cond (map? events-or-ids)
                        [(:id events-or-ids)]
                        (integer? events-or-ids)
                        [events-or-ids]
                        (map? (first events-or-ids))
                        (map :id events-or-ids)
                        :else
                        events-or-ids)]
    (some->> (sql
              (sql/select [*]
                (sql/from :registrations)
                (sql/where (list :in :event_id (seq event-ids)))))
             (jdbc/execute! db)
             (map db->registration))))

;;;
;;; Higher-level functions, creating nested relations
;;;

(defn nested-find-registrations
  [db event-or-events & subrecords]
  {:pre [(coll? event-or-events)]}
  (let [registrations (find-registrations-by-events db event-or-events)]
    (if (contains? (set subrecords) :user)
      (let [users-by-id (->> (set (map :user-id registrations))
                             (users/find-users-by-ids db)
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
      tournaments)))

(comment

  (defn get-next-tournament
    [db & [date]]
    (let [date (to-date (or date (now)))]
      (-> (base-tournaments-query db)
          (k/select
           (k/where {:date [> date]})
           (k/order :date :asc))
          first)))
  )
