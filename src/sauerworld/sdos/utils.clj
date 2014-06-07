(ns sauerworld.sdos.utils
  (:require [camel-snake-kebab :as csk]))

;; just use cond-> in future
(defn assoc-if
  [m cond k v]
  (let [assoc-fn (if (vector? k) assoc-in assoc)]
    (if cond
      (assoc-fn m k v)
      m)))

(defn map-keys [f m]
  (letfn [(mapper [[k v]] [(f k) (if (map? v) (map-keys f v) v)])]
    (into {} (map mapper m))))

(defn to-underscore-keys
  [m]
  (map-keys csk/->snake_case m))
