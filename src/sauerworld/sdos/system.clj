(ns sauerworld.sdos.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [sauerworld.sdos.db :as db]
            [immutant.web :as web])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defrecord Database [spec ^ComboPooledDataSource datasource]
  component/Lifecycle

  (start [this]
    (println ";; Starting database")
    (if connection ;; already started
      (do
        (println ";; Database already started")
        this)
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

(defrecord HttpServer [config site server]
  component/Lifecycle

  (start [this]
    (println ";; Starting HTTP server")
    (if server ;; already started
      this
      (let [serve-handle (web/run (:handler site) config)]
        (assoc this :server serve-handle))))

  (stop [this]
    (println ";; Stopping HTTP server")
    (if-not server ;; not running
      (do
        (println ";; HTTP server not running")
        this)
      (do
        (web/stop server)
        (assoc this :server nil)))))

(defn create-http-server
  [config]
  (map->HttpServer {:config config}))

(defrecord SdosSite [config handler db smtp]
  component/Lifecycle

  (start [this]
    (if handler ;; already started
      this
      (let [_ (assert (fn? (:handler options))
                      "SdosSite initialization error: Handler is not a function")
            ;; pass this to the handler in :app for deps
            handler #((:handler options) (assoc % :app this))]
        (assoc this :handler handler))))

  (stop [this]
    (if-not handler ;; not started
      this
      (assoc this :handler nil))))

(defn sdos-site [config]
  (map->SdosSite {:config config}))

(defn site [config]
  (let [{:keys [db http smtp site]} config]
    (component/system-map
     :db (create-database db)
     :smtp smtp
     :site (component/using
            (sdos-site site)
            [:db :smtp])
     :server (component/using
              (create-http-server http)
              [:site]))))
