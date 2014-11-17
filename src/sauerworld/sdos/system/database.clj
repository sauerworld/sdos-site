(ns sauerworld.sdos.system.database
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component])
  (:refer-clojure :exclude [read])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn make-pool
  "Create a connection pool from db spec."
  [{:keys [classname subprotocol subname user password
           excess-timeout idle-timeout minimum-pool-size maximum-pool-size
           test-connection-query
           idle-connection-test-period
           test-connection-on-checkin
           test-connection-on-checkout]
    :or   {excess-timeout (* 30 60)
           idle-timeout (* 3 60 60)
           minimum-pool-size 3
           maximum-pool-size 15
           test-connection-query nil
           idle-connection-test-period 0
           test-connection-on-checkin false
           test-connection-on-checkout false}
    :as   spec}]
  (doto (ComboPooledDataSource.)
    (.setDriverClass classname)
    (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
    (.setUser user)
    (.setPassword password)
    ;; expire excess connections after N minutes of inactivity
    (.setMaxIdleTimeExcessConnections excess-timeout)
    ;; expire all connections after N minutes of inactivity:
    (.setMaxIdleTime idle-timeout)
    (.setMinPoolSize minimum-pool-size)
    (.setMaxPoolSize maximum-pool-size)
    (.setIdleConnectionTestPeriod idle-connection-test-period)
    (.setTestConnectionOnCheckin test-connection-on-checkin)
    (.setTestConnectionOnCheckout test-connection-on-checkout)
    (.setPreferredTestQuery test-connection-query)))


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

(defrecord Database [config ^ComboPooledDataSource connection]
  component/Lifecycle

  (start [this]
    (println ";; Starting database")
    (if connection ;; already started
      (do
        (println ";; Database already started")
        this)
      (let [conn (make-pool config)]
        (assoc this :connection conn))))

  (stop [this]
    (println ";; Stopping database")
    (if (not connection) ;; already stopped
      this
      (do
        (try
          (.close connection)
          (catch Throwable t
            (log/warn t "Error when stopping database")))
        (assoc this :connection nil)))))

(defn new-database
  [config]
  (map->Database {:config config}))

(defn read
  [db query]
  (jdbc/query {:datasource (:connection db)} query))

(defn write
  [db query]
  (jdbc/execute! {:datasource (:connection db)} query))

(defn do-commands
  [db commands]
  (apply jdbc/db-do-commands {:datasource (:connection db)} commands))
