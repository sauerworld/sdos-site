(ns sauerworld.sdos.models.registrations
  (:require [clj-time.coerce :as tc]
            [honeysql.core :as sql]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.system.database :as db]))

(def ^{:private true} registration-key-spec
  {})

(def ^{:private true} registration->db-val-spec
  {:date tc/to-timestamp})

(def ^{:private true} ->registration-val-spec
  {:date tc/to-date-time})

(def ^{:private true} select-base
  {:select [:*]
   :from [:registrations]})

(defn db->registration
  [result]
  (model/->record result registration-key-spec ->registration-val-spec true))

(defn registration->db
  [registration]
  (model/->db registration registration-key-spec registration->db-val-spec))

(defn create
  [db registration]
  (->> {:insert-into :registrations
        :values [(registration->db registration)]}
       sql/format
       (db/write db)))

(defn find-by-ids
  [db ids]
  (some->> (assoc select-base
             :where [:in :id ids])
           sql/format
           (db/read db)
           (map db->registration)))

(defn find-by-events
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
    (some->> (assoc select-base
               :where [:in :event_id event-ids])
             sql/format
             (db/read db)
             (map db->registration))))

(defn find-by-user-and-event
  [db user-id event-id]
  (some->> (assoc select-base
             :where [:and [:= :user_id user-id] [:= :event_id event-id]]
             :limit 1)
           sql/format
           (db/read db)
           first
           db->registration))

(defn update
  [db registration]
  (->> {:update :tournaments
        :set (-> registration
                 registration->db
                 (dissoc :id))
        :where [:= :id (:id registration)]}
       sql/format
       (db/write db)))

(defn delete
  [db registration]
  {:pre [(or (integer? registration)
             (integer? (:id registration)))]}
  (let [id (or (:id registration) registration)]
    (->> {:delete-from :registrations
          :where [:= :id id]}
         sql/format
         (db/write db))))
