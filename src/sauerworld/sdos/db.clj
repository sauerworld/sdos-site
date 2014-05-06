(ns sauerworld.sdos.db
  (:require [clojure.string :as str]
            [sqlingvo.core :as q]))


(defn postgres
  "Create a database specification for a postgres database. Opts should include
  keys for :db, :user, and :password. You can also optionally set host and
  port."
  [{:keys [host port db]
    :or {host "localhost", port 5432, db ""}
    :as opts}]
  (merge {:classname "org.postgresql.Driver" ; must be in classpath
          :subprotocol "postgresql"
          :subname (str "//" host ":" port "/" db)}
         opts))

(defn h2
  "Create a database specification for a h2 database. Opts should include a key
  for :db which is the path to the database file."
  [{:keys [db]
    :or {db "h2.db"}
    :as opts}]
  (merge {:classname "org.h2.Driver" ; must be in classpath
          :subprotocol "h2"
          :subname db}
         opts))

(defn h2-memory
  "Creates specification of in-memory h2 database. Opts should include :db key."
  [{:keys [db]
    :or {db "h2"}
    :as opts}]
  (merge {:classname "org.h2.Driver"
          :subprotocol "h2"
          :subname (str "mem:" db)}
         opts))

(def articles-table
  (q/create-table :articles
    (q/column :id :serial :primary-key? true)
    (q/column :created-date :timestamp)
    (q/column :published-date :timestamp)
    (q/column :published :boolean)
    (q/column :title :text)
    (q/column :author :text)
    (q/column :category :text)
    (q/column :content :text)))

(def users-table
  (q/create-table :users
    (q/column :id :serial :primary-key? true)
    (q/column :username :text)
    (q/column :password :text)
    (q/column :validation_key :text)
    (q/column :validated :boolean)
    (q/column :pubkey :text)
    (q/column :created-date :timestamp)
    (q/column :admin :boolean)))

(def tournaments-table
  (q/create-table :tournaments
    (q/column :id :serial :primary-key? true)
    (q/column :start-date :timestamp)
    (q/column :end-date :timestamp)
    (q/column :name :text)
    (q/column :registration-open :boolean)))

(def events-table
  (q/create-table :events
    (q/column :id :serial :primary-key? true)
    (q/column :tournament-id :integer)
    (q/column :name :text)
    (q/column :mode :text)
    (q/column :team-mode :boolean)))

(def registrations-table
  (q/create-table :registrations
    (q/column :id :serial :primary-key? true)
    (q/column :event-id :integer)
    (q/column :user-id :integer)
    (q/column :team :text)
    (q/column :date :timestamp)))

(def registrations-index
  ["CREATE UNIQUE INDEX unique_reg ON registrations (user_id, event_id)"])

(comment


  (defn create-registrations-table
    [db-spec]
    (sql/db-do-commands db-spec
                        (ddl/create-table
                         :registrations
                         [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
                         [:event "integer"]
                         [:user "integer"]
                         [:team "varchar"]
                         [:created :timestamp])
                        (ddl/create-index :uniquereg :registrations
                                          [:event :user] :unique)))

  (def tables
    {:articles create-articles-table
     :users create-users-table})
  )
