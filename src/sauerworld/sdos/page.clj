(ns sauerworld.sdos.page
  (:require [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.models.articles :refer (article) :as articles]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.layout :refer (main-template error-template)]))

(defn page
  [category]
  (fn [req]
    (let [articles (articles/find-category-articles (get-in req [:app :db])
                                                    category)
          settings (get-settings req)]
      (main-template settings articles))))

(defn show-article
  [req]
  (let [id (some->
            (get-in req [:route-params :id])
            (Integer/parseInt))]
    (if-let [art (model/read (article id) (get-in req [:app :db]))]
      (let [settings (get-settings req)]
        (main-template settings [art]))
      {:status 404 :headers {} :body "Sorry, article not found."})))

(defn error-page
  [req message]
  (error-template (get-settings req)
                  (str message req)))
