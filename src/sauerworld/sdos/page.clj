(ns sauerworld.sdos.page
  (:require [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.models.articles :refer (find-category-articles
                                               find-article)]
            [sauerworld.sdos.layout :refer (main-template error-template)]
            [sauerworld.sdos.api :as api]))

(defn page
  [category]
  (fn [req]
    (let [articles (api/request :articles/find-category-articles category)
          settings (get-settings req)]
      (main-template settings articles))))

(defn show-article
  [req]
  (let [id (some->
            (get-in req [:route-params :id])
            (Integer/parseInt))]
    (if-let [article (api/request :articles/find-article id)]
      (let [settings (get-settings req)]
        (main-template settings [article]))
      {:status 404 :headers {} :body "Sorry, article not found."})))

(defn error-page
  [req message]
  (error-template (get-settings req)
                  (str message req)))
