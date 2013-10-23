(ns sauerworld.sdos.settings)

(def redirect-home
  {:status 302
   :headers {"Location" "/"}
   :body ""})

(def redirect-to-login
  {:status 302
   :headers {"Location" "/user/login"}
   :body ""})

(def base-url
  "http://dos.sauerworld.org")

(def links
  [["Home" "/"]
   ["About" "/about"]
   ["Events" "/events"]
   ["Next Tournament & Signup" "/tournaments/next"]
   ;;["Results" "/results"]
   ])

(def user-links
  [["Profile" "/user/"]
   ["Logout" "/user/logout"]
   ;;["Generate Auth Key" "/user/authkey"] ;; put this in when it's working
   ])

(def admin-links
  [["View/Edit Articles" "/admin/"]
   ["Add Article" "/admin/articles/create"]])

(def login-link
  ["Log in" "/user/login"])

(def registration-link
  ["Register" "/user/register"])

(def layout-settings
  {:email-title "Interested?"
   :email-subtitle "Sign up for email updates!"
   :menu-title "Menu"
   :user-menu-title "User"
   :admin-menu-title "Admin"
   :menu-items links
   :user-menu-items user-links
   :admin-menu-items admin-links})

(defn get-settings [req]
  (let [logged-in? (not (nil? (-> req :session :user)))
        admin? (true? (-> req :session :user :admin))
        menu-items (if logged-in?
                     links
                     (conj links login-link registration-link))]
    (merge layout-settings {:logged-in logged-in?
                            :admin admin?
                            :menu-items menu-items})))
