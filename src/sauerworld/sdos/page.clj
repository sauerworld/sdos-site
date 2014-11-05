(ns sauerworld.sdos.page
  (:require [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.models.articles :as articles]
            [sauerworld.sdos.layout :refer (main-template error-template)]))

(defn page
  [category]
  (fn [req]
    (let [articles (articles/get-in-category (get-in req [:app :db])
                                             category)
          settings (get-settings req)]
      (main-template settings articles))))

(defn show-article
  [req]
  (let [id (some->
            (get-in req [:route-params :id])
            (Integer/parseInt))]
    (if-let [art (articles/get-by-id id (get-in req [:app :db]))]
      (let [settings (get-settings req)]
        (main-template settings [art]))
      {:status 404 :headers {} :body "Sorry, article not found."})))

(defn error-page
  [req message]
  (error-template (get-settings req)
                  (str message req)))
