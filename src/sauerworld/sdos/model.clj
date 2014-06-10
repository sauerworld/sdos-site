(ns sauerworld.sdos.model
  (:require [clojure.set :as set]
            [sauerworld.sdos.utils :as utils]))

(defprotocol DatabaseRecord
  "Supports CRUD operations for a database record."
  (create [this db] "INSERT operation.")
  (read [this db] "SELECT operation.")
  (update [this db] "UPDATE operation.")
  (delete [this db] "DELETE operation."))

(defn ->db
  "Converts record keys according a the key-spec, a map of old key -> new key,
   values according to val-spec, a map of key -> new value fn, and then converts
   all dashes to underscore. Note the key conversion is done before the
   dash-to-underscore conversion, but after val conversion. The key-spec is
   mandatory, val-spec and invert-spec? are optional.

   If invert-key-spec? is true, the key-spec map is inverted via set/map-invert."
  ([record key-spec]
     (->db record key-spec nil false))
  ([record key-spec val-spec-or-invert]
     (if (map? val-spec-or-invert)
       (->db record key-spec val-spec-or-invert false)
       (->db record key-spec nil val-spec-or-invert)))
  ([record key-spec val-spec invert-key-spec?]
     (-> record
         (cond-> val-spec (utils/convert-map-vals val-spec))
         (set/rename-keys (cond-> key-spec invert-key-spec? set/map-invert))
         (utils/to-underscore-keys))))

(defn ->record
  "Converts a db result keys to a record according to a key-spec, a map of
   old key -> new key, values according to the val-spec, a map of
   key -> new value fn, and converts all underscores to dashes in key names.
   Note that the conversion order is: keys-to-dashes, key conversion, value
   conversion.

   If invert-key-spec? is true, the key-spec map is inverted via set/map-invert."
  ([result key-spec]
     (->record result key-spec nil nil))
  ([result key-spec val-spec-or-invert]
     (if (map? val-spec-or-invert)
       (->record result key-spec val-spec-or-invert nil)
       (->record result key-spec nil val-spec-or-invert)))
  ([result key-spec val-spec invert-key-spec?]
     (-> result
         (utils/to-dashed-keys)
         (set/rename-keys (cond-> key-spec invert-key-spec? set/map-invert))
         (cond-> val-spec (utils/convert-map-vals val-spec)))))
