(ns sauerworld.sdos.tournament
  (:require [sauerworld.sdos.api :refer (request)]
            [sauerworld.sdos.layout :refer (error-template app-page)]
            [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.views.tournament :as view]))

(defn show-tournament
  [req]
  (let [id (-> req :route-params :id Integer.)
        settings (get-settings req)
        user (-> req :session :user)]
    (if-let [tourney (request :tournaments/get-tournament id :events :registrations :users)]
      (app-page settings (view/tournament tourney user))
      (error-template settings
                      (str  "Tournament " id " not found.")))))

(defn show-next-tournament
  [req]
  (let [settings (get-settings req)
        next-t (request :tournaments/get-current-tournament)
        user (-> req :session :user)]
    (if next-t
      (let [tourney (request :tournaments/get-tournament next-t :events :registrations :users)]
        (app-page settings (view/tournament tourney)))
      (error-template settings
                      "Next tournament not yet scheduled."))))

(defn show-tournaments
  [req]
  (let [settings (get-settings req)]
    (if-let [tourneys (request :tournaments/get-tournaments :events)]
      (app-page settings (view/show-tournaments tourneys))
      (error-template settings
                      (str "No tournaments found.")))))

(defn show-signup-page
  [req event-id user]
  (let [settings (get-settings req)]
    (if-let [event (request :tournaments/get-event-by-id event-id)]
      (let [tournament (request :tournaments/get-tournament-for-event event)]
        (if (:registration_open tournament)
          (app-page settings
                    (view/signup-page (assoc event :tournament tournament)))
          (error-template settings "Registrations are not open for this event.")))
      (error-template settings "Event not found."))))

(defn edit-signup-page
  [req signup user]
  (let [settings (get-settings req)]
    (if-not (= (:user signup) (:id user))
      (error-template settings "You do not have access to edit this registration.")
      (let [event (request :tournaments/get-event-by-id (:event signup))
            tournament (request :tournaments/get-tournament-for-event event)]
        (if-not (:registration_open tournament)
          (error-template settings "Registrations are not open for this event.")
          (app-page settings
                    (view/signup-page
                     (assoc event :tournament tournament) signup)))))))

(defn event-signup
  [req]
  (let [user (-> req :session :user)
        event-id (-> req :route-params :id Integer.)]
    (if-let [signup (-> (request :tournaments/get-event-signups event-id user)
                        first)]
      (edit-signup-page req signup user)
      (show-signup-page req event-id user))))

(defn do-event-signup
  [req]
  (let [settings (get-settings req)
        user (-> req :session :user)
        event-id (-> req :route-params :id Integer.)
        event (request :tournaments/get-event-by-id event-id)
        dups? (first (request :tournaments/get-event-signups event-id user))
        tournament (request :tournaments/get-tournament-for-event event)]
    (if-not (:registration_open tournament)
      (error-template settings "Registrations are not open for this event.")
      (if dups?
        (error-template settings "You have aleady signed up for this event.")
        (let [signup {:event event-id
                      :user (:id user)
                      :team (-> req :params :team)}]
          (if (request :tournaments/insert-registration signup)
            (app-page settings "Registration successful!")
            (error-template settings
                            "Registration failed. Please try again later.")))))))

(defn do-edit-signup
  [req]
  (let [settings (get-settings req)
        event-id (-> req :route-params :id Integer.)
        signup-id (-> req :route-params :s-id Integer.)
        user (-> req :session :user)]
    (if-let [event (request :tournaments/get-events-by-id event-id)]
      (if-not (:team_mode event)
        (error-template settings "This is not a team mode - nothing to edit.")
        (let [tournament (request :tournaments/get-tournament-by-event event)]
          (if-not (:registration_open tournament)
            (error-template settings "Registrations are not open for this event.")
            (if-let [signup (->
                             (request :tournaments/get-event-signup signup-id user)
                             first)]
              (if-not (= (:id user) (:user signup))
                (error-template settings "You do not have permission to edit this registration.")
                (let [team (-> req :params :team)]
                  (if (request :tournaments/udpate-team signup team)
                    (app-page settings "Edit successful.")
                    (error-template settings "Unable to edit your registration. Please try again later."))))
              (error-template settings "Invalid signup.")))))
      (error-template settings "Unknown event, cannot edit signup."))))

(defn do-delete-signup
  [req]
  (let [settings (get-settings req)
        event-id (-> req :route-params :id Integer.)
        user (-> req :session :user)]
    (if-let [event (request :tournaments/get-event-by-id event-id)]
      (let [tournament (request :tournaments/get-tournament-for-event event)]
        (if-not (:registration_open tournament)
          (error-template settings "Event registration is closed for this tournament. You cannot delete your signup.")
          (let [signup (->
                        (request :tournaments/get-event-signups event-id user)
                        first)]
            (if-not (= (:id user) (:user signup))
              (error-template settings "You do not have permission to delete this signup.")
              (do
                (request :tournaments/delete-registration signup)
                (app-page settings "Signup deleted."))))))
      (error-template settings "Unknown event, cannot delete signup."))))
