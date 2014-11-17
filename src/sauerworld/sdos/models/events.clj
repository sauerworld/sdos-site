(ns sauerworld.sdos.models.events
  (:require [honeysql.core :as sql]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.system.database :as db]))

(def ^{:private true} event-key-spec
  {:team-mode? :team-mode})

(def ^{:private true} event->db-val-spec
  {})

(def ^{:private true} ->event-val-spec
  {})

(def ^{:private true} select-base
  {:select [:*]
   :from [:events]})

(defn db->event
  [result]
  (model/->record result event-key-spec ->event-val-spec true))

(defn event->db
  [event]
  (model/->db event event-key-spec event->db-val-spec))

(defn create
  [db event]
  {:pre [(integer? (:id event)) (integer? (:id event))]}
  (->> {:insert-into :events
        :values [(event->db event)]}
       sql/format
       (db/write db)))

(defn find-by-id
  [db id]
  (some->> (assoc select-base
             :where [:= :id id])
           (sql/format)
           (db/read db)
           first
           db->event))

(defn find-by-ids
  [db ids]
  (some->> (assoc select-base
             :where [:in :id ids])
           (sql/format)
           (db/read db)
           (map db->event)))

(defn find-by-tournaments
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
    (some->> (assoc select-base
               :where [:in :tournament_id tournament-ids])
             sql/format
             (db/read db)
             (map db->event))))

(defn update
  [db event]
  (db/write db
            (sql/format
             {:update :events
              :set (-> event
                       event->db
                       (dissoc :id))
              :where [:= :id (:id event)]})))

(defn delete
  [db event-or-id]
  (let [id (if (integer? event-or-id)
             event-or-id
             (:id event-or-id))]
    (db/write db
              (sql/format
               {:delete-from :events
                :where [:= :id id]}))))
