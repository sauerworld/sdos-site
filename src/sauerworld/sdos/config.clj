(ns sauerworld.sdos.config
  (:require [clojure.set :as set]
            [environ.core :refer (env)]
            [sauerworld.sdos.core :as site]
            [sauerworld.sdos.system.database :as db]))
(def db-types
  {"postgres" db/postgres
   "h2" db/h2})

(def db-spec
  (let [db-type (or (:db-type env) "postgres")
        config-fn (get db-types db-type)]
    (-> env
        (select-keys [:db-user :db-password :db-db
                      :db-classname :db-subprotocol :db-subname])
        (set/rename-keys {:db-user :user
                          :db-password :password
                          :db-db :db
                          :db-classname :classname
                          :db-subprotocol :subprotocol
                          :db-subname :subname})
        config-fn)))

(def http-server-conf
  {:port (or (:http-port env) 8080)
   :path (or (:http-path env) "/")})

(def smtp-conf
  {:host (:smtp-host env)
   :login (:smtp-login env)
   :password (:smtp-password env)
   :port 587
   :tls true})

(def app-conf
  {:handler site/app})

(def site-conf
  {:db db-spec
   :smtp smtp-conf
   :app app-conf
   :http http-server-conf})
