(ns sauerworld.sdos.views.tournament
  (:require [sauerworld.sdos.layout :as layout]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html]))

(layout/dev-setup *ns*)

(html/defsnippet tournament "templates/tournament.html"
  [:#tournament-view]
  [tournament & [user]]

  [:h3.name] (html/content (:name tournament))

  [:div.event]
  (html/clone-for [event (:events tournament)]

                  [:h4] (html/content (:name event))

                  [:li]
                  (if (:team_mode event)
                    (let [teams (group-by :team (:registrations event))]
                      (html/clone-for
                       [[name players] teams]
                       [:li]
                       (html/content
                        (str name " ("
                             (str/join
                              ", "
                              (map #(get-in % [:user :username]) players))
                             ")"))))
                    (html/clone-for
                     [player (:registrations event)]
                     [:li] (html/content (-> player :user :username))))

                  [:a]
                  (let [user-id (:id user)
                        registered? (->> (:registrations event)
                                         (filter #(= user-id (-> % :user :id)))
                                         empty?
                                         not)]
                    (when-not registered?
                      (let [link (str "/events/" (:id event) "/signup")]
                        (html/set-attr :href link))))))

(html/defsnippet show-tournaments "templates/tournament.html"
  [:#tournaments-summary]
  [tournaments]

  [:tbody :tr]
  (html/clone-for [tournament tournaments]

             [[:td (html/nth-of-type 1)]]
             (html/content (layout/format-date-time (:date tournament)))

             [[:td (html/nth-of-type 2)]]
             (html/content (html/html [:a {:href (str "/tournaments/" (:id tournament))}
                                       (:name tournament)]))

             [[:td (html/nth-of-type 3)]]
             (html/content (->> (:events tournament)
                                (map :name)
                                (str/join ", ")))))

(html/defsnippet delete-button "templates/tournament.html"
  [:#delete-button]
  [action]

  [:form] (html/set-attr :action action))

(html/defsnippet signup-page "templates/tournament.html"
  [:#signup-form]
  [event & [signup]]

  [:#team] (if (:team_mode event)
             (if signup
               (html/set-attr :value (:team signup))
               identity))

  [:.event] (html/content (:name event))

  [:.tournament] (html/content (get-in event [:tournament :name]))

  [:.date] (html/content (-> event :tournament :date layout/format-date-time))

  [:form] (if-not signup
             identity
             (html/after
              (delete-button
               (str "/events/" (:id event) "/signup/" (:id signup) "/delete")))))
