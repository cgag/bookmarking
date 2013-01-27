(ns bookmarking.handler
  (:require [bookmarking.auth :refer [authorized-user try-user
                                      valid-id? correct-user?]]
            [bookmarking.views.home :as home] [bookmarking.views.util :as util]
            [bookmarking.views.users :as users]
            [bookmarking.views.bookmarks :as bookmarks]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.models.user :as user-model]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.models.url :as url-model]
            [bookmarking.friend.custom-workflows :as custom-workflows]
            [compojure.core :refer :all]
            [compojure.response :as resp]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.set :as set]
            [clojure.string :as s]
            [environ.core :as e]
            [korma.db :refer :all]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows :as workflows]
             [credentials :as creds]
             [util :as friend-util]]))


(defroutes public-routes
  (GET "/" req 
       (try-user req
         (if user
           (ring.util.response/redirect (str "/users/" (:id user)))
           (home/home user))))
  (GET "/login" req 
       (try-user req
         (home/login user (select-keys req [:params]))))
  (GET "/register" req 
       (try-user req
         (home/register user)))
  (GET "/users/new" req
       (try-user req
         (home/register user)))
  (GET "/users" req 
       "get users"))

;; TODO: figure out something clean with the fact that I use -'s while korma expects _'s
;; TODO: if you attempt to post data when not logged in, you'll be prompted to log in, and then redirected,
;; but the actual post doesn't seem to happen? (bookmark won't be created)
;; -- actually, the redirect goes to the standard login redirect, so maybe the unauthorized-uri doesn't get set?
(defroutes strange-routes
  (POST "/:user-id/bookmarks"
        [user-id :as req]
        ;; TODO: ensure sure i can't be logged in as curtis and post to brian
        (try-user req
          (if-not user
            (do
              (home/login nil {:ref-req 
                               (select-keys req [:params 
                                                 :request-method 
                                                 :uri])}))
            (authorized-user user-id req
              (let [bookmark (bm-model/create! (:params req))]
                (if (:errors bookmark)
                  (bookmarks/new user-id (merge bookmark (:params req)))
                  (users/show user))))))))


(defroutes private-user-routes
  (GET "/" ;; make sure admin can access all of them
       [user-id :as req] 
       (authorized-user user-id req
         (users/show user (:params req))))
  (GET "/edit"
       [user-id :as req]
       (authorized-user user-id req
         (users/edit user-id)))
  (POST "/"
        [user-id :as req]
        (authorized-user user-id req
          (users/update! user-id))))

(defroutes private-bookmark-routes
  (GET "/bookmarks/new"
       [user-id :as req]
       (authorized-user user-id req
         (bookmarks/new user-id req)))
  ;; TODO: do a redirect instead
  (GET "/bookmarks"
       [user-id :as req]
       (authorized-user user-id req
         (users/show user (:params req))))
  (GET  "/bookmarks/:bookmark-id"
       [user-id bookmark-id :as req]
       (authorized-user user-id req
         (bookmarks/show user-id bookmark-id)))
  (GET  "/bookmarks/:bookmark-id/edit"
       [user-id bookmark-id :as req]
       (authorized-user user-id req
         (bookmarks/edit user-id bookmark-id)))
  ;; posting to bookmarks was here
  (POST "/bookmarks/:bookmark-id"
        [user-id bookmark-id :as req]
        (authorized-user user-id req
          (bookmarks/update! user-id bookmark-id)))
  ;; TODO: some sort of error handling if they tryt o delete something
  ;; that doesn't exist
  (POST "/bookmarks/:url-id/delete"
        [user-id url-id :as req]
        (authorized-user user-id req
          (let [bookmark (bm-model/find-bookmark user-id url-id)]
            (if-not bookmark
              "No such bookmark."
              (let [url (:url (url-model/by-id (:url_id bookmark)))]
                (bm-model/delete! user-id url-id)
                (str "Deleted bookmark for " url)))))))


(def successful-js
  "var d=document.createElement('div');
  d.setAttribute('style', 'position: fixed; width: 100%; height: 100%;' +
  'left: 0; top: 0; text-align: center; line-height: 100%;')
  d.style.background='black';
  d.style.color='white';
  d.style.fontFamily = 'Verdana';
  d.style.fontSize=60;
  d.innerHTML='<br/><br/><br/><br/><br/><br/><br/>Saving...'
  document.body.appendChild(d);

  var clearSaveDiv = function(){
  d.style.display='none';
  }

  setTimeout(clearSaveDiv, 500); 
  ")

(defn logged-out-js [params]
  (let [params (select-keys params [:url :userid :category :title])
        host (e/env :bm-host)
        port (e/env :bm-port)
        ssl-port (e/env :bm-ssl-port)]
    (str "document.location=\"" 
         "http://"
         host ":" port
         "/login?" 
         (util/query-str params)
         "\";")))

;; TODO: this allows you to save links to any user as long as you're logged in,
;; need something like strange-route i think
;; TODO: authorized user isn't good sincethis needs to return JS, infact, 
;; logged-out-js seems appropriate, just need to ditch the macro
(defroutes bookmarklet-route
  (GET "/js/bookmarklet.js"
       req
       (if (correct-user? req (:userid (:params req)))
         (if (:url (:params req))
           (let [bm-params (select-keys (:params req) [:url :userid :category :title])
                 bookmark (bm-model/create! (set/rename-keys bm-params {:userid :user-id}))] 
             (if (:errors bookmark)
               (str "alert('Error(s) saving bookmark: "
                    (s/join "," (:bookmark (:errors bookmark))) "');")
               successful-js))
           "alert('No url.');")
         (logged-out-js (:params req)))))


(defroutes slow-route
  (GET "/slow" req 
       (Thread/sleep 10000)
       (main-layout nil "Slow Page Title Woooo"
         [:p "BODY"])))


(defroutes app-routes
  slow-route
  public-routes
  (context ["/users/:user-id" :user-id #"[0-9]+"] req
           strange-routes
           (friend/wrap-authorize private-user-routes #{::user-model/user})
           (friend/wrap-authorize private-bookmark-routes #{::user-model/user}))
  bookmarklet-route
  (friend/logout (ANY "/logout" req (ring.util.response/redirect "/")))
  (route/files "/" {:root "resources"}))


(defn original-url
  [{:keys [scheme server-name server-port uri query-string]}]
  (str (name scheme) "://" server-name
       (cond
         (and (= :http scheme) (not= server-port 80)) (str \: server-port)
         (and (= :https scheme) (not= server-port 443)) (str \: server-port)
         :else nil)
       uri
       (when (seq query-string)
         (str \? query-string))))


(defn force-login-https [handler]
  (fn [req]
    (when (= (:uri req) "/login")
      (println "in force login https middleware")
      (println "uri: " (:uri req)) 
      (println "scheme: " (:scheme req) ) 
      (println "login req: " (pr-str req))) 
    (if (and (= (:uri req) "/login")
             (= (:scheme req) :http))
      (do
        (println "doing login https redirect to :" (original-url (assoc req
                                                                        :scheme :https
                                                                        :server-port "8443")))
        {:status 301 :headers {"Location" 
                               (original-url (assoc req
                                                    :scheme :https
                                                    :server-port :8443))}})
      (handler req))))


(def app
  (-> app-routes
    (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn user-model/credentials)
                          :workflows [;(workflows/interactive-form :login-uri "/")
                                      custom-workflows/registration
                                      custom-workflows/login]
                          :login-uri "/login"
                          :unauthorized-handler
                          (fn [& args]
                            {:status 401
                             :body "Access Denied"})})
    ;force-login-https
    handler/site))

