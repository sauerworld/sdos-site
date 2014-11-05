(ns sauerworld.sdos.rss
  (:require [sauerworld.sdos.settings :refer (base-url)]
            [sauerworld.sdos.models.articles :as articles]
            [clj-rss.core :refer (channel-xml)]
            [clj-time.coerce :refer (to-date)]
            [markdown.core :refer (md-to-html-string)]))

(defn rss-item
  [{:keys [id title author date content]}]
  (let [link (str base-url "/article/" id)]
    {:guid link
     :title title
     :description (md-to-html-string content)
     :link link
     :pubDate (to-date date)}))

(defn rss
  [req]
  (let [articles (articles/get-in-category (get-in req [:app :db]) "home")]
    (apply channel-xml
           {:title "Sauerbraten Day of Sobriety"
            :link base-url
            :description "A Sauerbraten Tournament"}
           (map rss-item articles))))
