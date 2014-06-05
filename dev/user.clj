(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.pprint :refer (pprint)]
            [clojure.test :as test]
            [clj-time.core :refer (date-time)]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [compojure.handler :refer (site)]
            [compojure.response :refer (render)]
            [postal.core :as mail]
            [environ.core :refer (env)]
            [sauerworld.sdos.system :refer (site-conf) :as system]
            [sauerworld.sdos.core :as core]
            [sauerworld.sdos.db :as db]
            [sauerworld.sdos.layout :as layout]
            [sauerworld.sdos.api :refer (request)]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as mr]))

;; mailgun smtp with ssl is port 587 - like amazon apparently



(def system nil)

(def init []
  (alter-var-root #'system (constantly (system/site site-conf))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset
  (stop)
  (refresh :after 'user/go))
