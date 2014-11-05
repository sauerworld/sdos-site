(ns sauerworld.sdos.system.app
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn wrap-resource
  [h resource-name resource]
  (fn [req]
    (h (assoc req resource-name resource))))

(defrecord SdosApp [config db smtp handler]
  component/Lifecycle

  (start [this]
    (if handler ;; already started
      this
      (let [_ (assert (fn? (:handler config))
                      "SdosSite initialization error: Handler is not a function")
            handler (-> (:handler config)
                        (wrap-resource :db db)
                        (wrap-resource :smtp smtp))]
        (assoc this :handler handler))))

  (stop [this]
    (if-not handler ;; not started
      this
      (assoc this :handler nil))))

(defn new-sdos-app [config]
  (map->SdosApp {:config config}))

(defn get-db
  [req]
  (:db req))

(defn get-smtp
  [req]
  (:smtp req))

(defn get-handler
  [app]
  (:handler app))
