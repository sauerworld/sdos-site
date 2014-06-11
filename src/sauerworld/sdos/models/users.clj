(ns sauerworld.sdos.models.users
  (:require [clojurewerkz.scrypt.core :as sc]
            [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [sauerworld.sdos.model :as model]
            [sqlingvo.core :refer (sql) :as sql]
            [clojure.java.jdbc :as jdbc])
  (:import [java.util UUID]))

(defn hash-password
  [pw]
  (sc/encrypt pw 16384 8 1))

(defn check-password
  [user pw]
  (sc/verify pw (:password user)))

(defn random-uuid
  []
  (str (java.util.UUID/randomUUID)))

(def ^{:private true} key-spec
  "Spec to convert User keys to db row."
  {:validated? :validated
   :admin? :admin})

(def ^{:private true} ->db-val-spec
  "Spec to convert User vals to db row."
  {:created-date tc/to-date})

(def ^{:private true} ->user-val-spec
  "Spec to convert db row vals to User."
  {:created-date tc/to-date-time})

(defrecord User
    [id username password
     validation-key validated? pubkey
     created-date admin?]

  model/DatabaseRecord
  (create [this db]
    (jdbc/execute! db
                   (sql
                    (sql/insert :users []
                      (sql/values
                       (model/->db this key-spec ->db-val-spec))))))
  (read [this db]
    {:pre [(or id username validation-key)]}
    ;; uses id, username or validation-key
    (let [query (sql (sql/select [*]
                       (sql/from :users)
                       (sql/limit 1)
                       (sql/where
                        (cond
                         id '(= :id id)
                         username '(= :username username)
                         validation-key '(= :validation_key validation-key)))))]
      (some-> (jdbc/execute! db query)
              first
              (model/->record key-spec ->user-val-spec true)
              (->> (merge this)))))
  (update [this db]
      {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/update :users
                        (-> this
                            (model/->db key-spec ->db-val-spec)
                            (dissoc :id))
                      (sql/where '(= :id id))))))
  (delete [this db]
    {:pre [id]}
    (jdbc/execute! db
                   (sql
                    (sql/delete :users
                      (sql/where '(= :id id)))))))

(defn user
  "Creates a User record, either from a map or an id.
   If no-hash? is true, password fields won't be hashed."
  [user-or-id & [no-hash?]]
  {:pre [(or (and (integer? user-or-id)
                  (pos? user-or-id))
             (map? user-or-id))]}
  (if (integer? user-or-id)
    (map->User {:id user-or-id})
    (let [defaults {:created-date (now)
                    :admin false}]
      (-> user-or-id
          (cond->
           (and (not no-hash?)
                (contains? user-or-id :password))
           (update-in [:password] hash-password))
          (->> (merge defaults))
          map->User))))

(defn db->user
  "Creates a User record from a database result."
  [result]
  (map->User (model/->record result key-spec ->user-val-spec true)))

(defn update-password
  "Updates a User record with a new password, hashing it."
  [user new-password]
  (assoc user :password (hash-password new-password)))

(defn find-all-users
  [db]
  (some->> (sql
            (sql/select [*]
              (sql/from :users)))
           (jdbc/execute! db)
           (map db->user)))
