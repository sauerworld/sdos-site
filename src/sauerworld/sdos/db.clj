(ns sauerworld.sdos.db
  (:require [clojure.string :as str]))


(defn postgres
  "Create a database specification for a postgres database. Opts should include
  keys for :db, :user, and :password. You can also optionally set host and
  port."
  [{:keys [host port db]
    :or {host "localhost", port 5432, db ""}
    :as opts}]
  (merge {:classname "org.postgresql.Driver" ; must be in classpath
          :subprotocol "postgresql"
          :subname (str "//" host ":" port "/" db)}
         opts))

(defn h2
  "Create a database specification for a h2 database. Opts should include a key
  for :db which is the path to the database file."
  [{:keys [db]
    :or {db "h2.db"}
    :as opts}]
  (merge {:classname "org.h2.Driver" ; must be in classpath
          :subprotocol "h2"
          :subname db}
         opts))

(defn h2-memory
  "Creates specification of in-memory h2 database. Opts should include :db key."
  [{:keys [db]
    :or {db "h2"}
    :as opts}]
  (merge {:classname "org.h2.Driver"
          :subprotocol "h2"
          :subname (str "mem:" db)}
         opts))
