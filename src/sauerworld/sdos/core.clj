(ns sauerworld.sdos.core
  (:require [sauerworld.sdos.settings :refer :all]
            [sauerworld.sdos.page :as page]
            [sauerworld.sdos.tournament :as t]
            [sauerworld.sdos.user :as user]
            [sauerworld.sdos.admin :as admin]
            [sauerworld.sdos.rss :refer (rss)]
            [environ.core :refer (env)]
            [compojure.core :refer :all]
            [compojure.route :refer (not-found) :as route]
            [compojure.response :refer (render)]
            [immutant.web :as web]
            [immutant.util :refer (at-exit)]
            [compojure.handler :refer (site)]
            [clojure.tools.logging :refer (error)]
            [clojure.stacktrace :refer (print-cause-trace)]))

(def world (atom {}))

(defn test-request
  [req]
  (str req))

(defroutes admin-routes
  (GET "/" [] admin/show-articles-summary)
  (GET "/articles/create" [] admin/create-article-page)
  (POST "/articles" [] admin/do-create-article)
  (GET "/articles/:id" [] admin/edit-article-page)
  (POST "/articles/:id" [] admin/do-edit-article)
  (POST "/articles/:id/delete" [] admin/do-delete-article)
  (GET "/users" [] admin/show-users)
  (GET "/users/new" [] admin/add-user-page)
  (POST "/users" [] admin/do-add-user)
  (GET "/users/:id" [] admin/show-user)
  (POST "/users/:id" [] admin/do-edit-user)
  (POST "/users/:id/delete" [] admin/do-delete-user)
  (GET "/tournaments" [] admin/show-tournaments)
  (GET "/tournaments/new" [] admin/add-tournament-page)
  (POST "/tournaments" [] admin/do-add-tournament)
  (GET "/tournaments/:id" [] admin/show-tournament)
  (POST "/tournaments/:id" [] admin/do-edit-tournament))

(defroutes user-routes
  (GET "/" [] user/profile-page)
  (GET "/login" [] user/login-page)
  (POST "/login" [] user/do-login)
  (GET "/register" [] user/registration-page)
  (POST "/register" [] user/do-registration)
  (GET "/validate/:validation-key" [] user/validate-email)
  (GET "/logout" [] user/do-logout)
  (GET "/password" [] user/password-page)
  (POST "/password" [] user/do-password)
  (GET "/validate/resend" [] user/resend-validation)
  (GET "/authkey" [] (user/wrap-require-validation user/authkey-page))
  (POST "/authkey" [] (user/wrap-require-validation user/do-authkey)))

(defroutes event-routes
  (GET "/:id/signup" [] t/event-signup)
  (POST "/:id/signup" [] t/do-event-signup)
  (POST "/:id/signup/:s-id" [] t/do-edit-signup)
  (POST "/:id/signup/:s-id/delete" [] t/do-delete-signup))

(defroutes app-routes
  (GET "/" [] (page/page "home"))
  (GET "/about" [] (page/page "about"))
  (GET "/events" [] (page/page "events"))
  (GET "/article/:id" [] page/show-article)
  (GET "/rss" [] rss)
  (GET "/tournaments/next" [] t/show-next-tournament)
  (GET "/tournaments" [] t/show-tournaments)
  (GET "/tournaments/:id" [] t/show-tournament)
  (context "/events" [] (-> event-routes
                            user/wrap-require-validation
                            user/wrap-require-user))
  (GET "/test" [] test-request)
  (context "/admin" [] (admin/wrap-require-admin admin-routes))
  (context "/user" [] (user/wrap-require-user
                       user-routes
                       :except ["login"
                                "register"
                                "validate"]))
  (not-found "Sorry buddy, page not found!"))

(defn wrap-throwable-errors
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (do
          (error (with-out-str (print-cause-trace t)))
          (-> (page/error-page req t)
                (render req)))))))

(def app (-> app-routes
             site))

(def app-repl (-> app-routes
                  site))
