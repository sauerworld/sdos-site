(ns sauerworld.sdos.api
  (:require [immutant.messaging :as msg]))

(def api-queue
  "queue/storage")

(defn start-api
  [& [queue]]
  (let [queue (or queue api-queue)]
    (msg/start queue)))

(defn request
  "Performs storage api requests. Throws Throwable on error."
  [action & params]
  (let [api-res (msg/request "queue/storage" {:action action :params params})
        timeout-exception (Throwable. "Error accessing database -- request timed out.")
        response-map (deref api-res 2000 nil)]
    (if-not (nil? response-map)
      (let [{status :status response :response} response-map]
        (if (= :error status)
          (throw (Throwable. "Database access error. Unknown request."))
          response))
      (throw timeout-exception))))
