(ns sauerworld.sdos.models.users
  (:require [clj-time.coerce :as tc]
            [clj-time.core :refer (now)]
            [clojurewerkz.scrypt.core :as sc]
            [honeysql.core :as sql]
            [validateur.validation :as v]
            [sauerworld.sdos.model :as model]
            [sauerworld.sdos.system.database :as db])
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

(def ^{:private true} select-base
  {:select [:*]
   :from [:users]})

;; User contains fields:
;; id username password validation-key validated? pubkey created-date admin?

(defn db->user
  "Creates a user map from a database result."
  [result]
  (model/->record result key-spec ->user-val-spec true))

(defn user->db
  "Creates database-formatted record from a user map."
  [user]
  (model/->db user key-spec ->db-val-spec))

(defn create
  [db user]
  (-> {:insert-into :users
       :values [(user->db user)]}
      sql/format
      (->> (db/write db))))

(defn get-one-id
  "Gets a single user by id."
  [db id]
  {:pre [(integer? id)]}
  (-> select-base
      (assoc :where [:= :id id]
             :limit 1)
      sql/format
      (->> (db/read db)
           first
           db->user)))

(defn get-ids
  "Gets multiple users by ids."
  [db ids]
  (-> select-base
      (assoc :where [:in :id ids])
      sql/format
      (->> (db/read db)
           (map db->user))))

(defn get-by-id
  "Gets user records by id. If id-or-ids is a collection, gets multiple."
  [db id-or-ids]
  {:pre [(and (integer? id-or-ids)
              (pos? id-or-ids))
         (or (coll? id-or-ids))]}
  (if (integer? id-or-ids)
    (get-one-id db id-or-ids)
    (get-ids db id-or-ids)))

(defn get-by-validation-key
  [db validation-key]
  (-> select-base
      (assoc :where [:= :validation_key validation-key]
             :limit 1)
      sql/format
      (->> (db/read db)
           first
           db->user)))

(defn get-by-username
  [db username]
  (-> select-base
      (assoc :where [:= :username username]
             :limit 1)
      sql/format
      (->> (db/read db)
           first
           db->user)))

(defn get-all
  [db]
  (some->> (sql/format select-base)
           (db/read db)
           (map db->user)))

(defn update
  [db user]
  {:pre [(:id user)]}
  (db/write db
            (sql/format
             {:update :users
              :set (-> user
                       user->db
                       (dissoc :id) )
              :where [:= :id (:id user)]})))

(defn update-password
  "Updates a User record with a new password, hashing it."
  [db id new-password]
  (update db {:id id :password (hash-password new-password)}))

(defn delete
  [db user-or-id]
  {:pre [(or (and (integer? user-or-id)
                  (pos? user-or-id))
             (map? user-or-id))]}
  (when-let [id (if (integer? user-or-id)
                  user-or-id
                  (:id user-or-id))]
    (db/write db
              (sql/format
               {:delete-from :users
                :where [:= :id id]}))))

(defn check-login
  [db username password]
  (when-let [user (get-by-username db username)]
    (when (check-password user password)
      user)))


;;; Validation

(defn match-of
  "Creates validation function that specifies that two attributes must be the
   same (for example, for password & password confirmation fields."
  [attribute1 attribute2]
  (let [getter (fn [k] (fn [m] (if (vector? k)
                                 (get-in m k)
                                 (get m k))))
        f1 (getter attribute1)
        f2 (getter attribute2)]
    (fn [m]
      (let [[v1 v2] ((juxt f1 f2) m)
            res (= v1 v2)
            msg (str "must match " attribute2)
            errors (if res {} {attribute1 #{msg}})]
        [(empty? errors) errors]))))

(defn uniqueness-of
  "Validation function that checks that a value unique. Takes a key to retrieve
   the value, and a function that, when passed the value, will report its
   uniqueness. Like, for instance, a database query function."
  [attribute check-fn]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [value (f m attribute)
            unique? (check-fn value)
            msg (str value " not available, already taken.")
            errors (if unique? {} {attribute #{msg}})]
        [(empty? errors) errors]))))

(def password-match
  (match-of :password :password-confirm))

(defn make-registration-validator
  [db]
  (v/validation-set
   (v/presence-of :email)
   (v/presence-of :password)
   password-match
   (v/presence-of :username)
   (uniqueness-of :username #(->> %
                                 (get-by-username db)
                                 nil?))))

(defn validate-registration
  [db registration]
  (-> db make-registration-validator registration))

(def password-validator
  (v/validation-set
   (v/presence-of :password)
   password-match))

(defn validate-password
  [params]
  (password-validator params))
