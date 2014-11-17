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
            [environ.core :refer (env)]
            [honeysql.core :as sql]
            [postal.core :as mail]
            [sauerworld.sdos.system :as system]
            [sauerworld.sdos.config :refer (site-conf)]
            [sauerworld.sdos.core :as core]
            [sauerworld.sdos.layout :as layout]
            [sauerworld.sdos.system.database :as db]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as mr]))

;; mailgun smtp with ssl is port 587 - like amazon apparently



(def system nil)

(defn init []
  (alter-var-root #'system (constantly (system/site site-conf))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))


;;; Plan to proceed

;; Rip out the guts of the over-complicated "model" system, make simple queries.
;; Later can replace it with new db library if so inclined.
;;
;; Move towards getting the user/signup code into a separate thing, so it can be
;; part of sauerworld proper
;;
;; Maybe an api.sauerworld.org or something for these functions

(defmacro current-work
  []
  '(do
     (require 'migrate)
     (def h2s (migrate/new-h2-spec "resources/db/main"))
     (def dbrec (-> (db/new-database h2s) component/start))



     )
  )
