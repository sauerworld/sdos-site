(ns sauerworld.sdos.config
  (:require [environ.core :refer (env)]
            [sauerworld.sdos.core :as site]))

(def db-spec
  {:classname (:db-classname env)
   :subprotocol (:db-subprotocol env)
   :subname (:db-subname env)
   :user (:db-user env)
   :password (:db-password env)})

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
   :site app-conf
   :http http-server-conf})
