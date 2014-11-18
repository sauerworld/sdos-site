(ns sauerworld.sdos.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [sauerworld.sdos.system.database :as db]
            [sauerworld.sdos.system.app :as app]
            [immutant.web :as web])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn select-with-merge
  "Selects subset of a larger configuration map, merging in the selected keys.
  Selection can be a vector, merge-keys must be a vector. In a conflict, the
  merge-keys take precedence."
  [config selection merge-keys]
  (let [get-or-in (fn [m k]
                    (if (coll? k)
                      (get-in m k)
                      (get m k)))
        subset (get-or-in config selection)]
    (reduce (fn [m k]
              (assoc m k (get-or-in config k)))
            subset
            merge-keys)))

(defn with-dev
  "Selects a subkey of the config map, merging in the dev flag."
  [config key]
  (select-with-merge config key [:dev]))

(defrecord HttpServer [config app server]
  component/Lifecycle

  (start [this]
    (println ";; Starting HTTP server")
    (if server ;; already started
      this
      (let [options (select-keys config [:port])
            handler (app/get-handler app)
            serve-handle (if (:dev config)
                           (web/run-dmc handler options)
                           (web/run handler options))]
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
              (new-http-server (with-dev config :http))
              [:app]))))
