(ns sauerworld.sdos.models.tournaments
  (:require [clj-time.core :refer (now)]
            [clj-time.coerce :as tc]
            [sauerworld.sdos.model :as model]
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

(comment

  (defn get-next-tournament
    [db & [date]]
    (let [date (to-date (or date (now)))]
      (-> (base-tournaments-query db)
          (k/select
           (k/where {:date [> date]})
           (k/order :date :asc))
          first)))

  (defn get-tournaments
    [db]
    (-> (base-tournaments-query db)
        (k/select)))

  (defn get-tournament-events
    [db tournament]
    {:pre [(number? (:id tournament))]}
    (let [id (:id tournament)]
      (-> (base-events-query db)
          (k/select
           (k/where {:tournament id})))))

  (defn get-event-signups
    [db event]
    {:pre [(number? (:id event))]}
    (let [id (:id event)]
      (-> (base-registrations-query db)
          (k/select
           (k/where {:event id})))))

  (defn get-tournament-signups
    [db tournament]
    (let [id (if (number? tournament)
               (int tournament)
               (-> tournament :id int))
          events (get-tournament-events db id)

          ]
      (-> (base-registrations-query db)
          (k/select
           (k/join :inner
                   (k/create-entity "events")
                   (= :events.id :event))
           (k/join (k/create-entity "users")
                   (= :users.id :user))
           (k/where {:events.tournament id})))))


  )
