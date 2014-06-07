(defproject sauerworld/sdos "0.3.0-SNAPSHOT"
  :description "A website for the Sauerbraten Day of Sobriety"
  :url "dos.sauerworld.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies [[org.clojars.jcrossley3/tools.namespace "0.2.4.1"]
                             [ring-mock "0.1.5"]
                             [criterium "0.4.3"]
                             [com.h2database/h2 "1.4.178"]]
              :plugins [[lein-environ "0.5.0"]]}
             :test {:dependencies [[com.h2database/h2 "1.4.178"]]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.immutant/web "2.x.incremental.131"]
                 [clj-time "0.7.0"]
                 [clj-rss "0.1.3"]
                 [camel-snake-kebab "0.1.5"]
                 [environ "0.5.0"]
                 [com.draines/postal "1.11.1"]
                 [liberator "0.11.0"]
                 [compojure "1.1.6"]
                 [enlive "1.1.5"]
                 [markdown-clj "0.9.41"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [sqlingvo "0.5.21"]
                 ;; do not upgrade ragtime until github issue #34 is resolved
                 [ragtime "0.3.6"
                  :exclusions [org.clojure/java.jdbc]]
                 [c3p0/c3p0 "0.9.1.2"]
                 [postgresql/postgresql "9.3-1101.jdbc41"]
                 [sauerworld/cube2.crypto "0.9.1-SNAPSHOT"]
                 [com.h2database/h2 "1.4.178"]
                 [clojurewerkz/scrypt "1.1.0"]
                 [com.novemberain/validateur "1.7.0"]]
  :repositories [["Immutant 2.x incremental builds"
                  "http://downloads.immutant.org/incremental/"]]
  :ragtime {:migrations ragtime.sql.files/migrations}
  :plugins [[ragtime/ragtime.lein "0.3.6"]])
