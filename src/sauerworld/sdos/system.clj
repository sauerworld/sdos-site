(ns sauerworld.sdos.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [sauerworld.sdos.system.database :as db]
            [sauerworld.sdos.system.app :as app]
            [immutant.web :as web])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defrecord HttpServer [config app server]
  component/Lifecycle

  (start [this]
    (println ";; Starting HTTP server")
    (if server ;; already started
      this
      (let [serve-handle (web/run (app/get-handler app) config)]
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

(defn new-http-server
  [config]
  (map->HttpServer {:config config}))

(defn site [config]
  (let [{:keys [db http smtp app]} config]
    (component/system-map
     :db (db/new-database db)
     :smtp smtp
     :app (component/using
            (app/new-sdos-app app)
            [:db :smtp])
     :server (component/using
              (new-http-server http)
              [:app]))))
