(ns sauerworld.sdos.models.articles
  (:require [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.system.database :as db]
            [honeysql.core :as sql]))

(def ^{:private true} key-spec
  "Spec to convert Article keys to db row."
  {:published? :published})

(def ^{:private true} ->db-val-spec
  "Spec to convert Article vals to db row."
  {:created-date tc/to-timestamp
   :published-date tc/to-timestamp})

(def ^{:private true} ->article-val-spec
  "Spec to convert db row vals to Article."
  {:created-date tc/to-date-time
   :published-date tc/to-date-time})

(def ^{:private true} select-base
  {:select [:*]
   :from [:articles]
   :order-by [[:published_date :desc]]})

;; Articles have the following fields:
;; id created-date published-date published? category title author content

(defn article->db
  [article]
  (-> article
      (cond->
       (not (contains? article :created-date))
       (assoc :created-date (now)))
      (model/->db key-spec ->db-val-spec)))

(defn db->article
  [result]
  (model/->record result key-spec ->article-val-spec true))

(defn create
  [db article]
  (db/write db
            (sql/format
             {:insert-into :articles
              :values [(article->db article)]})))

(defn find-by-id
  [db id]
    {:pre [(and (integer? id)
                (pos? id))]}
  (->> (sql/format
        (assoc select-base
          :limit 1
          :where [:= :id id]))
       (db/read db)
       first
       (db->article)))

(defn find-by-category
  "Find all articles in a given category."
  [db category]
  (->> (sql/format
        (assoc select-base
          :where [:= :category category]))
       (db/read db)
       (map db->article)))

(defn find-all
  [db]
  (->> (sql/format select-base)
       (db/read db)
       (map db->article)))

(defn update
  [db article]
  (db/write db
            (sql/format
             {:update :articles
              :set (-> article
                       article->db
                       (dissoc :id))
              :where [:= :id (:id article)]})))

(defn delete
  [db article-or-id]
  {:pre [(or (and (integer? article-or-id)
                  (pos? article-or-id))
             (map? article-or-id))]}
  (when-let [id (if (integer? article-or-id)
                  article-or-id
                  (:id article-or-id))]
    (db/write db
              (sql/format
               {:delete-from :articles
                :where [:= :id id]}))))
