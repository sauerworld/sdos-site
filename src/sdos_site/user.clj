(ns sdos-site.user
  (:require [clojure.string :as str]
            [sdos-site.settings :refer :all]
            [sdos-site.layout :as layout]
            [sdos-site.views.user :as view]
            [sdos-site.db :as db]))

(defn wrap-validation
  [h error-view-fn & opts]
  (fn [{:keys [session] :as req}]
    (let [opts-map (apply hash-map opts)
          uri (:uri req)
          excepts (:except opts-map)
          exception? (when excepts
                       (not empty?
                            (-> (re-pattern (str/join "||" excepts))
                                (re-find uri))))
          validated (-> session :user :validated)]
      (if exception?
        (h req)
        (if validated
          (h req)
          (error-view-fn (str "<p>Your email address has not yet been validated.</p>"
                              "<p><a href=\"/user/validate/resend\">Click here "
                              "to resend validation email.</a></p>")))))))

(defn profile-page
  [req]
  (let [db (:db req)
        user (-> req :session :user)
        profile-snippet (view/user-profile user)]))

(defn login-page
  [req]
  "login page")

(defn do-login
  [req]
  "do login")

(defn do-logout
  [req]
  "do logout")

(defn registration-page
  [req]
  "registration page")

(defn do-registration
  [req]
  "do registration")

(defn password-page
  [req]
  "password page")

(defn do-password
  [req]
  "do password")

(defn resend-validation
  [req]
  "resend validation")

(defn validate-email
  [req]
  "validate email")

(defn authkey-page
  [req]
  "authkey page")

(defn do-authkey
  [req]
  "do authkey")

(defn signup-page
  [req]
  "signup page")

(defn do-signup
  [req]
  "do signup")

(defn show-signup
  [req]
  "show signup")

(defn do-edit-signup
  [req]
  "do edit signup")