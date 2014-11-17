(ns sauerworld.sdos.tournament
  (:require [sauerworld.sdos.api :refer (request)]
            [sauerworld.sdos.layout :refer (error-template app-page)]
            [sauerworld.sdos.settings :refer (get-settings)]
            [sauerworld.sdos.system.app :as app]
            [sauerworld.sdos.models.tournaments :as tournaments]
            [sauerworld.sdos.models.events :as events]
            [sauerworld.sdos.models.registrations :as registrations]
            [sauerworld.sdos.views.tournament :as view]))

(defn show-tournament
  [req]
  (let [id (-> req :route-params :id Integer.)
        settings (get-settings req)
        db (app/get-db req)
        user (-> req :session :user)]
    (if-let [tourney (tournaments/get-tournament-with db id :events :registrations :users)]
      (app-page settings (view/tournament tourney user))
      (error-template settings
                      (str  "Tournament " id " not found.")))))

(defn show-next-tournament
  [req]
  (let [settings (get-settings req)
        db (app/get-db req)
        next-t (tournaments/find-next db)
        user (-> req :session :user)]
    (if (seq next-t)
      (let [tourney (tournaments/get-tournament-with db (:id next-t) :events :registrations :users)]
        (app-page settings (view/tournament tourney)))
      (error-template settings
                      "Next tournament not yet scheduled."))))

(defn show-tournaments
  [req]
  (let [settings (get-settings req)
        db (app/get-db req)]
    (if-let [tourneys (tournaments/find-all db)]
      (let [events (events/find-by-tournaments db tourneys)
            tourneys-w-events (map (fn [t]
                                     (tournaments/combine-tournament-entities {:tournament t :events events}))
                                   tourneys)]
        (app-page settings (view/show-tournaments tourneys-w-events)))
      (error-template settings
                      (str "No tournaments found.")))))

(defn show-signup-page
  [req event-id user]
  (let [settings (get-settings req)
        db (app/get-db req)]
    (if-let [event (events/find-by-id db event-id)]
      (let [tournament (tournaments/find-by-id db (:tournament-id event))]
        (if (:registration-open tournament)
          (app-page settings
                    (view/signup-page (assoc event :tournament tournament)))
          (error-template settings "Registrations are not open for this event.")))
      (error-template settings "Event not found."))))

(defn edit-signup-page
  [req registration user]
  (let [settings (get-settings req)
        db (app/get-db req)]
    (if-not (= (:user-id registration) (:id user))
      (error-template settings "You do not have access to edit this registration.")
      (let [event (events/find-by-id db (:event-id registration))
            tournament (tournaments/find-by-id db (:tournament-id event))]
        (if-not (:registration-open tournament)
          (error-template settings "Registrations are not open for this event.")
          (app-page settings
                    (view/signup-page
                     (assoc event :tournament tournament) registration)))))))

(defn event-signup
  [req]
  (let [user (-> req :session :user)
        event-id (-> req :route-params :id Integer.)
        db (app/get-db req)]
    (if-let [registration (registrations/find-by-user-and-event db (:id user) event-id)]
      (edit-signup-page req registration user)
      (show-signup-page req event-id user))))

(defn do-event-signup
  [req]
  (let [settings (get-settings req)
        user-id (-> req :session :user :id Integer/parseInt)
        event-id (-> req :route-params :id Integer.)
        db (app/get-db req)
        event (events/find-by-id db event-id)
        dup? (registrations/find-by-user-and-event db user-id event-id)
        tournament (tournaments/find-by-id db (:tournament-id event))]
    (if-not (:registration-open tournament)
      (error-template settings "Registrations are not open for this event.")
      (if dup?
        (error-template settings "You have aleady signed up for this event.")
        (let [registration {:event-id event-id
                            :user-id user-id
                            :team (-> req :params :team)}]
          (if (registrations/create db registration)
            (app-page settings "Registration successful!")
            (error-template settings
                            "Registration failed. Please try again later.")))))))

(defn do-edit-signup
  [req]
  (let [settings (get-settings req)
        event-id (-> req :route-params :id Integer.)
        registration-id (-> req :route-params :s-id Integer.)
        user-id (-> req :session :user :id Integer/parseInt)
        db (app/get-db req)]
    (if-let [event (events/find-by-id db event-id)]
      (if-not (:team-mode event)
        (error-template settings "This is not a team mode - nothing to edit.")
        (let [tournament (tournaments/find-by-id db (:tournament-id event))]
          (if-not (:registration-open tournament)
            (error-template settings "Registrations are not open for this event.")
            (if-let [registration (registrations/find-by-user-and-event db user-id event-id)]
              (if-not (= user-id (:user-id registration))
                (error-template settings "You do not have permission to edit this registration.")
                (let [team (-> req :params :team)]
                  (if (registrations/update db {:id (:id registration)
                                                :team team})
                    (app-page settings "Edit successful.")
                    (error-template settings "Unable to edit your registration. Please try again later."))))
              (error-template settings "Invalid signup.")))))
      (error-template settings "Unknown event, cannot edit signup."))))

(defn do-delete-signup
  [req]
  (let [settings (get-settings req)
        event-id (-> req :route-params :id Integer.)
        user-id (-> req :session :user :id Integer/parseInt)
        db (app/get-db req)]
    (if-let [event (events/find-by-id db event-id)]
      (let [tournament (tournaments/find-by-id db (:tournament-id event))]
        (if-not (:registration-open tournament)
          (error-template settings "Event registration is closed for this tournament. You cannot delete your signup.")
          (let [registration (registrations/find-by-user-and-event db user-id event-id)]
            (if-not (= user-id (:user-id registration))
              (error-template settings "You do not have permission to delete this signup.")
              (do
                (registrations/delete db registration)
                (app-page settings "Signup deleted."))))))
      (error-template settings "Unknown event, cannot delete signup."))))
