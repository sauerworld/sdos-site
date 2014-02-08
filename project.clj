(defproject sauerworld/sdos "0.1.0-SNAPSHOT"
  :description "A website for the Sauerbraten Day of Sobriety"
  :url "dos.sauerworld.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies [[org.clojars.jcrossley3/tools.namespace "0.2.4.1"]
                             [org.immutant/immutant "1.1.0"]
                             [ring-mock "0.1.5"]]
              :immutant {:nrepl-port 0}}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-time "0.6.0"]
                 [clj-rss "0.1.3"]
                 [environ "0.4.0"]
                 [com.draines/postal "1.11.1"]
                 [compojure "1.1.6"]
                 [enlive "1.1.5"]
                 [markdown-clj "0.9.41"]
                 [korma "0.3.0-RC6"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [sauerworld/cube2.crypto "0.9.1-SNAPSHOT"]
                 [com.h2database/h2 "1.3.170"]
                 [clojurewerkz/scrypt "1.1.0"]
                 [com.novemberain/validateur "1.7.0"]]
  :immutant {:init "sauerworld.sdos.core/initialize"
             :context-path "/"})
