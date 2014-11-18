(ns sauerworld.sdos.page
  (:require [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.models.articles :as articles]
            [sauerworld.sdos.layout :refer (main-template error-template)]
            [sauerworld.sdos.system.app :as app]))

(defn page
  [category]
  (fn [req]
    (let [db (app/get-db req)
          articles (articles/find-by-category db category)
          settings (get-settings req)]
      (main-template settings articles))))

(defn show-article
  [req]
  (let [id (some->
            (get-in req [:route-params :id])
            (Integer/parseInt))
        db (app/get-db req)]
    (if-let [art (articles/find-by-id db id)]
      (let [settings (get-settings req)]
        (main-template settings [art]))
      {:status 404 :headers {} :body "Sorry, article not found."})))

(defn error-page
  [req message]
  (error-template (get-settings req)
                  (str message req)))
