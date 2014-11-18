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

(defn to-dashed-keys
  [m]
  (map-keys csk/->kebab-case m))

(defn convert-map-vals
  "m is a map to convert. spec is a map of key -> val-fn. If key is a vector,
   refers to nested map keys."
  [m spec]
  (reduce (fn [m [k f]]
            (let [contains-key?
                  (cond (coll? k)
                        (contains? (get-in m (drop-last k)) (last k))
                        :default (contains? m k))
                  update-key (if (coll? k) k [k])]
              (if contains-key?
                (update-in m update-key f)
                m)))
          m
          spec))
