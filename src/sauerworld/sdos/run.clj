(ns sauerworld.sdos.run
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer (env)]
            [sauerworld.sdos.core :as sdos]
            [sauerworld.sdos.system :as system]
            [sauerworld.sdos.system.database :as db]))

(def dev-config
  {:http {:port 8080}
   :db (db/postgres {:username "sauerworld" :password "sauerworld" :db "sauerworld"})
   :app {:handler sdos/app}
   :smtp {}
   :dev true})

(defn -main
  "Application entry point"
  [& args]
  (let [conf (if (:dev env)
               dev-config
               env)
        sys (system/site conf)]
    (println "Starting system.")
    (component/start sys)))
