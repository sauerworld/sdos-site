(ns migrate
  (:require [clojure.java.io :as io]
            [clj-time.core :as tme]
            [clj-time.coerce :as tc]
            [com.stuartsierra.component :as component]
            [honeysql.core :as sql]
            [ragtime.sql.files :as rt]
            [sauerworld.sdos.system.database :as db]
            [sauerworld.sdos.models.articles :as articles]
            [sauerworld.sdos.models.events :as events]
            [sauerworld.sdos.models.registrations :as registrations]
            [sauerworld.sdos.models.tournaments :as tournaments]
            [sauerworld.sdos.models.users :as users]))

;; Here's the old h2 schema

(def oldschema
  "The old h2 schema, for reference."
  {:articles
   [[:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:date :timestamp]
    [:title "varchar"]
    [:author "varchar"]
    [:content "varchar"]
    [:category "varchar"]]
   :users
   [[:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:username "varchar"]
    [:password "varchar"]
    [:email "varchar"]
    [:validation_key "varchar"]
    [:validated "boolean"]
    [:pubkey "varchar"]
    [:created :timestamp]
    [:admin "boolean"]]
   :tournaments
   [[:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:date :timestamp]
    [:name "varchar"]
    [:registration_open "boolean"]]
   :events
   [[:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:tournament "integer"]
    [:name "varchar"]
    [:team_mode "boolean"]]
   :registrations
   [[:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:event "integer"]
    [:user "integer"]
    [:team "varchar"]
    [:created :timestamp]]})

(defn new-h2-spec
  [file]
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname file})

(defn ensure-schema
  [db]
  (let [sql-file (io/resource "schema/001-basic.sql")
        statements (rt/sql-statements (slurp sql-file))]
    (try
      (db/do-commands db statements)
      (catch Throwable t ()))))

;; functions to migrate individual tables from h2 to postgres

(defn migrate-articles
  [h2-db pg-db]
  (let [old-arts (db/read h2-db
                          (sql/format {:select [:*] :from [:articles]}))]
    (doseq [art old-arts]
      (-> art
          (assoc :created-date (:date art)
                 :published-date (:date art)
                 :published true)
          (dissoc :date)
          (->> (articles/create pg-db))))))

(defn migrate-users
  [h2-db pg-db]
  (let [old-users (db/read h2-db
                           (sql/format {:select [:*] :from [:users]}))]
    (doseq [user old-users]
      (-> user
          (assoc :created-date (:created user))
          (dissoc :created)
          (->> (users/create pg-db))))))

(defn migrate-tournaments
  [h2-db pg-db]
  (let [old-tournaments (db/read h2-db
                                 (sql/format {:select [:*] :from [:tournaments]}))]
    (doseq [tourney old-tournaments]
      (-> tourney
          (assoc :start-date (:date tourney)
                 :end-date (tme/plus (tc/to-date-time (:date tourney))
                                     (tme/hours 8)))
          (dissoc :date)
          (->> (tournaments/create pg-db))))))

(defn migrate-events
  [h2-db pg-db]
  (let [old-events (db/read h2-db
                            (sql/format {:select [:*] :from [:events]}))]
    (doseq [event old-events]
      (-> event
          (assoc :mode (:name event)
                 :tournament-id (:tournament event))
          (dissoc :tournament)
          (->> (events/create pg-db))))))

(defn migrate-registrations
  [h2-db pg-db]
  (let [old-registrations (db/read h2-db
                                   (sql/format {:select [:*] :from [:registrations]}))]
    (doseq [registration old-registrations]
      (-> registration
          (assoc :event-id (:event registration)
                 :user-id (:user registration)
                 :date (:created registration))
          (dissoc :event :user :created)
          (->> (registrations/create pg-db))))))

(defn migrate-all
  [h2-db pg-db]
  (migrate-articles h2-db pg-db)
  (migrate-users h2-db pg-db)
  (migrate-tournaments h2-db pg-db)
  (migrate-events h2-db pg-db)
  (migrate-registrations h2-db pg-db))

(defn migrate-with-defaults
  []
  (let [h2 (-> (db/h2 {:db "db/main"})
               db/new-database
               component/start)
        pg (-> (db/postgres {:db "sauerworld"
                             :user "sauerworld"
                             :password "sauerworld"}))]
    (ensure-schema pg)
    (migrate-all h2 pg)))
