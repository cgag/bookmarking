(ns bookmarking.handler
  (:require [compojure.core :refer :all]
            [compojure.response :as resp]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [korma.db :refer :all]
            [bookmarking.auth :refer [authorized-user try-user]]
            [bookmarking.views.home :as home]
            [bookmarking.views.util :as util]
            [bookmarking.views.users :as users]
            [bookmarking.views.bookmarks :as bookmarks]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.models.user :as user-model]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.models.url :as url-model]
            [bookmarking.friend.custom-workflows :as custom-workflows]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows   :as workflows]
                             [credentials :as creds]
                             [util :as friend-util]]))

;; TODO: should it redirect to /u or something like instapaper?  Bad semanticlaly
;; to have "/" do different things?

(defroutes public-routes
  (GET "/" req 
       (try-user req
         (if user
           (ring.util.response/redirect (str "/users/" (:id user)))
           (home/home user))))
  (GET "/login" req 
       (println "in /login route: " req)
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
;; TODO: maybe have to avoid friend for this?
;; Ideas:  post, if not logged in then log in, and then do the post request
(defroutes strange-routes
  (POST "/:user-id/bookmarks"
        [user-id :as req]
        ;; TODO: user something like authorized user but do all this
        ;; instead of throwing unauthorized
        ;; TODO: Or maybe authorize user should just use this code and
        ;; render login this way? What would we miss out on?
        ;; TODO: ensure sure i can't be logged in as curtis and post to brian
        (try-user req
          (if-not user
            (do
              (println "No user, rendering login page with ref-req: " req)
              (home/login nil {:ref-req 
                               (select-keys req [:params 
                                                 :request-method 
                                                 :uri])}))
            (authorized-user user-id req
              (let [bookmark (bm-model/create! (:params req))]
                (if (:errors bookmark)
                  (bookmarks/new user-id (merge bookmark (:params req)))
                  (users/show user))))))))

;(defroutes strange-routes
  ;(POST "/:user-id/bookmarks"
        ;[user-id :as req]
        ;(println "matched post to bookmarks")
        ;(authorized-user user-id req
          ;(let [bookmark (bm-model/create! (:params req))]
            ;(if (:errors bookmark)
              ;(bookmarks/new user-id (merge bookmark (:params req)))
              ;(users/show user))))))

(defroutes private-user-routes
  (GET "/:user-id" ;; make sure admin can access all of them
       [user-id :as req] 
       (authorized-user user-id req
         (users/show user (:params req))))
  (GET "/:user-id/edit"
       [user-id :as req]
       (authorized-user user-id req
         (users/edit user-id)))
  (POST "/:user-id"
        [user-id :as req]
        (authorized-user user-id req
          (users/update! user-id))))

(defroutes private-bookmark-routes
  (GET "/:user-id/bookmarks/new"
       [user-id :as req]
       (authorized-user user-id req
         (bookmarks/new user-id req)))
  ;; TODO: do a redirect instead
  (GET "/:user-id/bookmarks"
       [user-id :as req]
       (authorized-user user-id req
         (users/show user (:params req))))
  (GET  "/:user-id/bookmarks/:bookmark-id"
       [user-id bookmark-id :as req]
       (authorized-user user-id req
         (bookmarks/show user-id bookmark-id)))
  (GET  "/:user-id/bookmarks/:bookmark-id/edit"
       [user-id bookmark-id :as req]
       (authorized-user user-id req
         (bookmarks/edit user-id bookmark-id)))
  ;; posting to bookmarks was here
  (POST "/:user-id/bookmarks/:bookmark-id"
        [user-id bookmark-id :as req]
        (authorized-user user-id req
          (bookmarks/update! user-id bookmark-id)))
  ;; TODO: some sort of error handling if they tryt o delete something
  ;; that doesn't exist
  (POST "/:user-id/bookmarks/:url-id/delete"
        [user-id url-id :as req]
        (println "matched post delete route")
        (authorized-user user-id req
          (let [bookmark (bm-model/find-bookmark user-id url-id)]
            (if-not bookmark
              "No such bookmark."
              (let [url (:url (url-model/by-id (:url_id bookmark)))]
                (bm-model/delete! user-id url-id)
                (str "Deleted bookmark for " url)))))))


;"var d=document.createElement('div');
;o.setAttribute('id', 'ovipb614671');
;var textStyle = 
;'-webkit-text-size-adjust: none; ' +
;'font-family: Helvetica, Arial, sans-serif; font-weight: bold; ' +
;'line-height: 1.0; letter-spacing: normal; font-variant: normal; font-style: normal;'
;;
;o.setAttribute('style',
                 ;'position: fixed; z-index: 2147483647; left: 0; top: 0; width: 100%; height: 100%; font-size: 30px; ' +
                 ;'opacity: 0; -webkit-transition: opacity 0.25s linear; text-align: center; ' +
                 ;'padding: 200px 0 0 0; margin: 0; background-color: #000; color: #ccc; ' +
                 ;textStyle)
;document.body.appendChild(o);" 

(def successful-js
  "var d=document.createElement('div');
  d.setAttribute('style', 'position: fixed; width: 100%; height: 100%;' +
  'left: 0; top: 0; text-align: center; line-height: 100%;')
  d.style.background='black';
  d.style.color='white';
  d.innerHTML='<br/><br/><br/><br/><br/><br/><br/>Saving...'
  document.body.appendChild(d);
  
  var clearSaveDiv = function(){
    d.style.display='none';
  }
  
  setTimeout(clearSaveDiv, 400); 
  ")

(defn logged-out-js [params]
  (let [params (select-keys params [:url :userid :category :title])]
    (str "document.location=\"http://localhost:3000/login?" 
         (util/query-str params)
         "\";")))

(defroutes bookmarklet-route
  (GET "/js/bookmarklet.js"
       req
       (try-user req
         (if-not user
           (logged-out-js (:params req))
           (if (:url (:params req))
             (let [bm-params (select-keys (:params req) [:url :category :title])
                   bookmark (bm-model/create! (merge bm-params 
                                                     {:user-id (:id user)}))] 
               (if (:errors bookmark)
                 (str "alert('Error(s) saving bookmark: " (apply str (interpose "," (:bookmark (:errors bookmark)))) "');")
                 successful-js))
             "alert('No url.');")))))

;(home/login nil {:ref-req 
                 ;(select-keys req [:params 
                                   ;:request-method 
                                   ;:uri])})
;
;; TODO: use context for :user-id portion as well, and enforce #"[0-9]+"
;; TODO: why am i required to be logged in to register?
(defroutes app-routes
  public-routes
  (context "/users" req
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


;; TODO: redirect to users bookmarks after logging in
;; -> probably just need custom workflow w/ a redirect?
;; -> or somehow customize redirect url, sure it's just a map entry
;; TODO: chaneg login-uri?
(def app
  (-> app-routes
    (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn user-model/credentials)
                          :workflows [;(workflows/interactive-form :login-uri "/")
                                      custom-workflows/registration
                                      custom-workflows/login]
                          :login-uri "/login"
                          :unauthorized-handler
                          (fn [& args]
                            {:status 200 
                             :body (str "ACCESS DENIED:\n" (pr-str args) "\n\n"
                                        "Required roles: " )})})
    ;force-login-https
    handler/site))
