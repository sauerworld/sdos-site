(ns sauerworld.sdos.models.articles
  (:require [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [clojure.java.jdbc :as jdbc]
            [sauerworld.sdos.model :as model]
            [honeysql.core :as sql]))

(def ^{:private true} key-spec
  "Spec to convert Article keys to db row."
  {:published? :published})

(def ^{:private true} ->db-val-spec
  "Spec to convert Article vals to db row."
  {:created-date tc/to-date
   :published-date tc/to-date})

(def ^{:private true} ->article-val-spec
  "Spec to convert db row vals to Article."
  {:created-date tc/to-date-time
   :published-date tc/to-date-time})

(def ^{:private true} select-base
  {:select [*]
   :from :articles})

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
  (jdbc/execute! db
                 (sql/format
                  {:insert-into :articles
                   :values [(article->db article)]})))

(defn get-by-id
  [db id]
    {:pre [(and (integer? id)
                (pos? id))]}
  (->> (sql/format
        (assoc select-base
          :limit 1
          :where [:= :id id]))
       (jdbc/execute! db)
       first
       (db->article)))

(defn get-in-category
  "Find all articles in a given category."
  [db category]
  (->> (sql/format
        (assoc select-base
          :where [:= :category category]))
       (jdbc/execute! db)
       (map db->article)))

(defn get-all
  [db]
  (->> (sql/format select-base)
       (jdbc/execute! db)
       (map db->article)))

(defn update
  [db article]
  (jdbc/execute! db
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
    (jdbc/execute! db
                   (sql/format
                    {:delete-from :articles
                     :where [:= :id id]}))))
