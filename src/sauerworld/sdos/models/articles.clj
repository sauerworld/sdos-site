(ns sauerworld.sdos.models.articles
  (:require [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [clojure.java.jdbc :as jdbc]
            [sauerworld.sdos.utils :as utils]
            [sqlingvo.core :refer (sql) :as sql]))

(defn find-category-articles
  "Find all articles by category."
  [db category]
  (jdbc/execute! db
                 (sql
                  (sql/select [*]
                    (sql/from :articles)
                    (sql/where '(= :category category))))))

(defn find-all-articles
  [db]
  (jdbc/execute! db
                 (sql
                  (sql/select [*]
                    (sql/from :articles)))))

(defn find-article
  "Find single article by id."
  [db article-id]
  (->
   (jdbc/execute! db
                 (sql
                  (sql/select [*]
                    (sql/from :articles)
                    (sql/limit 1)
                    (sql/where '(= :id article-id)))))
   first))

(defn add-article
  "Inserts an article."
  [db article]
  (jdbc/execute! db
                 (sql
                  (sql/insert :articles []
                    (sql/values
                     (-> article
                         (cond-> :created-date
                                 (update-in :created-date tc/to-date)
                                 (complement :created-date)
                                 (assoc :created-date (java.util.Date.))
                                 :published-date (tc/to-date))
                         utils/to-underscore-keys))))))

(defn update-article
  "Updates an article."
  [db article]
  (jdbc/execute! db
                 (sql
                  (sql/update :articles (dissoc article :id)
                    (sql/where '(= :id (:id article)))))))
