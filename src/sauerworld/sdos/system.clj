(ns sauerworld.sdos.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [sauerworld.sdos.db :as db])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defrecord Database [spec ^ComboPooledDataSource datasource]
  component/Lifecycle

  (start [this]
    (println ";; Starting database")
    (if connection ;; already started
      this
      (let [conn (db/make-pool spec)]
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

(defn create-database
  [spec]
  (map->Database {:spec spec}))
