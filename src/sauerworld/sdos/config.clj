(ns sauerworld.sdos.config
  (:require [environ.core :refer (env)]))

(def db-spec
  {:classname (:db-classname env)
   :subprotocol (:db-subprotocol env)
   :subname (:db-subname env)
   :user (:db-user env)
   :password (:db-password env)})

(def http-server
  {:port (or (:http-port env) 8080)})
