(ns sauerworld.sdos.user
  (:require [clojure.string :as str]
            [sauerworld.sdos.settings :refer :all]
            [sauerworld.sdos.layout :as layout]
            [sauerworld.sdos.views.user :as view]
            [sauerworld.sdos.models.users :as users]
            [sauerworld.sdos.email :refer (send-email)]
            [sauerworld.sdos.system.app :as app]
            [sauerworld.cube2.crypto :as crypto]
            [compojure.response :refer (render)]
            [clojure.tools.logging :refer (info)]))

(defn send-validation-email
  [server email validation]
  (let [validation-link (str "http://dos.sauerworld.org/user/validate/"
                             validation)
        body [:alternative
              {:type "text/plain"
               :content (str
                         "
Thanks for registering your account for Saurbraten: Day of Sobriety.
To validate your email address, please go here:" validation-link)}
              {:type "text/html"
               :content (str
                         "
<p>Thanks for registering your account for Saurbraten: Day of Sobriety.
To validate your email address, please click on:</p>

<p><a href=\"" validation-link "\">" validation-link "</a></p>")}]
        message {:to email
                 :from "Sauerbraten Day of Sobriety <noreply@dos.sauerworld.org>"
                 :subject "Site Registration Email Address Validation"
                 :body body}]
    (send-email server message)))

(defn error-strings
  [errors]
  (mapcat (fn [[k v]]
            (map #(str (name k) " " %) v))
          errors))

(defn wrap-require-user
  [h & opts]
  (fn [req]
    (let [opts-map (apply hash-map opts)
          uri (:uri req)
          excepts (:except opts-map)
          exception? (when excepts
                       (not (empty?
                             (-> (re-pattern (str/join "|" excepts))
                                 (re-find uri)))))
          user? (-> req :session :user)]
      (if (or exception? user?)
        (h req)
        (-> (layout/error-template (get-settings req)
                                   (str "<p>You need to be logged in for this action.</p>"
                                        "<p><a href=\"/user/login\">Click here to log in."
                                        "</a></p>"))
            (render req))))))

(defn wrap-require-validation
  [h & opts]
  (fn [req]
    (let [opts-map (apply hash-map opts)
          uri (:uri req)
          excepts (:except opts-map)
          exception? (when excepts
                       (not (empty?
                             (-> (re-pattern (str/join "|" excepts))
                                 (re-find uri)))))
          validated? (-> req :session :user :validated)]
      (if (or exception? validated?)
        (h req)
        (-> (layout/error-template (get-settings req)
                                   (str "<p>Your email address has not yet been validated.</p>"
                                        "<p><a href=\"/user/validate/resend\">Click here "
                                        "to resend validation email.</a></p>"))
            (render req))))))

(defn profile-page
  [req]
  (let [user (-> req :session :user)]
    (layout/app-page (get-settings req) (view/user-profile user))))

(defn login-page
  [req]
  (layout/app-page (get-settings req) (view/login-page)))

(defn do-login
  [req]
  (do
    (let [username (-> req :params :username)
          password (-> req :params :password)]
      (if-let [user (users/check-login (app/get-db req) username password)]
        (assoc-in redirect-home [:session :user] user)
        (layout/app-page (get-settings req)
                         (view/login-page "Invalid username or password."))))))

(defn do-logout
  [req]
  {:session {:user nil}
   :status 302
   :headers {"Location" "/"}
   :body ""})

(defn registration-page
  [req]
  (layout/app-page (get-settings req)
                   (view/registration-page)))

;; TODO: fix this, what a nightmare
(defn do-registration
  [req]
  (let [{:keys [username password password-confirm email]} (:params req)
        db (app/get-db req)
        registration {:username username
                      :password password
                      :password-confirm password-confirm
                      :email email}
        smtp (app/get-smtp req)
        validation (users/validate-registration db registration)]
    (if-not (empty? validation) ;; escapes first
      (layout/app-page (get-settings req)
                       (view/registration-page (assoc registration
                                                 :error
                                                 (error-strings validation))))
      (do
        (users/create db registration)
        (let [newuser (users/find-by-username db username)]
          (if (send-validation-email smtp email (:validation_key newuser))
            (layout/app-page (get-settings req)
                             (view/registration-thanks))
            (let [error-msg
                  "There was a problem sending your account validation email.
                   Please try again later."]
              (layout/error-template (get-settings req)
                                     error-msg))))))))

(defn password-page
  [req]
  (layout/app-page (get-settings req)
                   (view/password-page)))

(defn do-password
  [req]
  (let [password (-> req :params :password)
        password-confirm (-> req :params :password-confirm)
        validation (users/validate-password (:params req))]
    (if-not (nil? validation)
      ;; validation error case
      (layout/app-page (get-settings req)
                       (view/password-page
                        {:error (error-strings validation)}))
      ;; validation ok case
      (let [user (-> req :session :user)]
        (if (users/update-password (app/get-db req) user password)
          (layout/app-page (get-settings req)
                           (view/success-page))
          (let [error-msg
                "There was a problem setting your new password.
                 Please try again later."]
            (layout/error-template (get-settings req)
                                   error-msg)))))))

(defn resend-validation
  [req]
  (let [smtp (app/get-smtp req)
        user (-> req :session :user)
        email (:email user)
        validation (:validation-key user)
        required [["Email server" email] ["user" user]
                  ["email address" email] ["validation key" validation]]]
    (if (and user email validation smtp)
      (if (send-validation-email smtp email)
        (layout/app-page (get-settings req)
                         (view/success-page (str "Validation email resent to "
                                                 email)))
        (layout/error-template (get-settings req)
                               "There was a problem sending your validation
                                email. Please try again later."))
      (let [missing-fields (->> required
                                (filter #(nil? (second %)))
                                (map first)
                                (str/join ", ")
                                (str "Error - missing fields: "))]
        (layout/error-template (get-settings req)
                               missing-fields)))))

(defn validate-email
  [req]
  (let [settings (get-settings req)
        submitted-key (-> req :route-params :validation-key)
        db (app/get-db req)]
    (if-let [user (users/find-by-validation-key db submitted-key)]
      (do
        (users/update db (assoc user :validated? true))
        (let [session-user (-> req :session :user)
              body (layout/app-page settings (str "Email validated."))]
          (if (= (:id user) (:id session-user))
            (let [session (:session req)]
              {:session (assoc-in session [:user :validated?] true)
               :body body})
            body)))
      (layout/error-template settings
                             "Sorry, email validation key not found."))))

(defn authkey-page
  [req]
  (let [user (-> req :session :user)]
    (layout/app-page (get-settings req) (view/authkey-page user))))

(defn do-authkey
  [req]
  (let [db (app/get-db req)
        user (-> req :session :user)
        privkey (crypto/make-privkey)
        pubkey (crypto/get-pubkey privkey)
        cs-string (str "authkey \""
                       (:username user) "\" \""
                       privkey
                       "\" \"sauerworld.org\" \nautoauth 1 \nsaveauthkeys\n")]
    (users/update db (assoc user :pubkey pubkey))
    (->
     {:status 200
      :headers {"Content-Type" "text/plain"
                "Content-Disposition" "attachment; filename=\"once.cfg\""}
      :body cs-string}
     (assoc-in [:session :user] (assoc user :pubkey pubkey)))))

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
