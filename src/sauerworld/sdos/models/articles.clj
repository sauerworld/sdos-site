(ns sauerworld.sdos.models.articles
  (:require [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [clojure.java.jdbc :as jdbc]
            [sauerworld.sdos.model :as model]
            [sqlingvo.core :refer (sql) :as sql]))

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

(defrecord Article
    [id created-date published-date published?
     category title author content]

  model/DatabaseRecord
  (create [this db]
    (jdbc/execute! db
                   (sql
                    (sql/insert :articles []
                      (sql/values
                       (model/->db this key-spec ->db-val-spec))))))
  (read [this db]
    (some->
     (jdbc/execute! db
                    (sql
                     (sql/select [*]
                       (sql/from :articles)
                       (sql/limit 1)
                       (sql/where '(= :id article-id)))))
     first
     (model/->record key-spec ->article-val-spec true)
     (->> (merge this))))
  (update [this db]
      (jdbc/execute! db
                     (sql
                      (sql/update :articles
                          (-> this
                              (model/->db key-spec ->db-val-spec)
                              (dissoc :id))
                        (sql/where '(= :id (:id this)))))))
  (delete [this db]
    (jdbc/execute! db
                   (sql
                    (sql/delete :articles
                      (sql/where '(= :id (:id this))))))))

(defn article
  "Creates an Article record, either from a map or an id."
  [article-or-id]
  {:pre [(or (and (integer? article-or-id)
                  (pos? article-or-id))
             (map? article-or-id))]}
  (if (integer? article-or-id)
    (map->Article {:id article-or-id})
    (-> article-or-id
        (cond->
         (not (contains? article-or-id :created-date))
         (assoc :created-date (now)))
        map->Article)))

(defn db->article
  [result]
  (map->Article (model/->record result key-spec ->article-val-spec true)))

(defn find-category-articles
  "Find all articles by category."
  [db category]
  (->> (sql
        (sql/select [*]
          (sql/from :articles)
          (sql/where '(= :category category))))
       (jdbc/execute! db)
       (map db->article)))

(defn find-all-articles
  [db]
  (->> (sql
        (sql/select [*]
          (sql/from :articles)))
       (jdbc/execute! db)
       (map db->article)))
