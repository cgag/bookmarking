(ns bookmarking.handler (:require [bookmarking.auth :refer [authorized-user try-user
                                      valid-id? correct-user?]]
            [bookmarking.views.home :as home]
            [bookmarking.views.util :as util]
            [bookmarking.views.users :as users]
            [bookmarking.views.categories :as categories]
            [bookmarking.views.bookmarks :as bookmarks]
            [bookmarking.views.boilerpipe :as bp-views]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.models.user :as user-model]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.models.url :as url-model]
            [bookmarking.models.category :as cat-model]
            [bookmarking.friend.custom-workflows :as custom-workflows]
            [compojure.core :refer :all]
            [compojure.response :as resp]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.set :as set]
            [clojure.string :as s]
            [environ.core :as e]
            [korma.db :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.server.standalone :as ring-server]
            [cemerick.friend :as friend]
            [cemerick.friend [workflows :as workflows]
             [credentials :as creds]
             [util :as friend-util]])
  (:gen-class))

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
;; TODO: stop wrapping return map in {:bookmark <>} or {:errors}, just
;; add key to result of create
(defroutes create-bookmark
  (POST "/bookmarks" [user-id :as req]
    (try-user req
        (if-not user
          (home/login nil {:ref-req 
                           (select-keys req [:params 
                                             :request-method 
                                             :uri])})
          (authorized-user user-id req
            (let [bookmark (bm-model/create! (:params req))]
              (if (:errors bookmark)
                (bookmarks/new user (merge bookmark (:params req)))
                (ring.util.response/redirect
                 (str "/users/" user-id "/categories/"
                      (:category_id (:bookmark bookmark)))))))))))


(defroutes private-user-routes
  ;; make sure admin can access all of them
  (GET "/" [user-id :as req] 
    (authorized-user user-id req
      (users/show user
                  (:id (cat-model/first-category user-id))
                  (:params req))))
  
  (GET "/edit" [user-id :as req]
    (authorized-user user-id req
      (users/edit user)))
  (POST "/" [user-id :as req]
    (authorized-user user-id req
      (let [updated (user-model/update! user (:params req))
            errors  (:errors updated)]
        (if errors
          (users/edit user errors)
          (ring.util.response/redirect (str "/users/" (:id user))))))))

(defroutes private-bookmark-routes
  (GET "/bookmarks/new" [user-id :as req]
    (authorized-user user-id req
      (bookmarks/new user req)))
  (GET "/bookmarks" [user-id :as req]
    (authorized-user user-id req
      (ring.util.response/redirect
       (str "/users/" user-id
            "/categories/" (:id (cat-model/first-category user-id))))))
  (GET "/categories/:cat-id/bookmarks/:url-id/edit" [user-id cat-id url-id :as req]
    (authorized-user user-id req
      (bookmarks/edit user cat-id url-id)))
  (POST "/bookmarks/:url-id" [user-id url-id :as req]
    (authorized-user user-id req
      (let [params (:params req)
            current-cat (:current-cat params)]
        (bm-model/update! user-id url-id params)
        (ring.util.response/redirect (str "/users/" user-id "/categories/" current-cat)))))
  (POST "/categories/:cat-id/bookmarks/:url-id/delete" [user-id cat-id url-id :as req]
    (authorized-user user-id req
      (let [bookmark (bm-model/find-bookmark user-id url-id cat-id)]
        (if-not bookmark
          "No such bookmark."
          (let [url (:url (url-model/by-id (:url_id bookmark)))]
            (bm-model/delete! user-id url-id cat-id)
            (str "Deleted bookmark for " url)))))))


(defroutes private-category-routes
  (GET "/categories" [user-id :as req]
    (authorized-user user-id req
      (categories/manage-categories-view user)))
  (GET "/categories/new" [user-id :as req]
    (authorized-user user-id req
      (categories/new user)))
  (GET "/categories/:cat-id" [user-id cat-id :as req]
    (authorized-user user-id req
      (if (cat-model/has-category-id? user-id cat-id)
        (users/show user cat-id (:params req))
        "No such category")))
  (GET "/categories/:cat-id/edit" [user-id cat-id :as req]
    (authorized-user user-id req
      (categories/edit-category user cat-id)))
  (GET "/categories/:cat-id/search" [user-id cat-id :as req]
    (authorized-user user-id req
      (users/search-results user cat-id (:params req))))
  (POST "/categories" [user-id :as req]
    (authorized-user user-id req
      (let [cat-name (:category (:params req))
            category (cat-model/create! user-id cat-name)]
        (if (:errors category)
          (categories/new user category)
          (ring.util.response/redirect (str "/users/" (:id user)))))))
  (POST "/categories/:cat-id" [user-id cat-id :as req]
    (authorized-user user-id req
      (let [updated-cat (cat-model/update! user-id cat-id (:params req))
            errors (:errors updated-cat)]
        (if errors
          (categories/edit-category user cat-id errors)
          (ring.util.response/redirect (str "/users/" user-id))))))
  (POST "/categories/:cat-id/delete" [user-id cat-id :as req]
    (authorized-user user-id req
      (cat-model/delete! user-id cat-id))))


;;TODO: Move to separate file
(def successful-js
  "var d=document.createElement('div');
  d.setAttribute('style', 'position: fixed; width: 100%; height: 100%;' +
  'left: 0; top: 0; text-align: center; line-height: 100%;')
  d.style.background='black';
  d.style.color='white';
  d.style.fontFamily = 'Oswald, sans-serif';
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
        port (e/env :bm-port)]
    (str "document.location=\"" 
         "http://"
         host ":" port
         "/login?" 
         (util/query-str params)
         "\";")))


(defroutes bookmarklet-route
  (GET "/js/bookmarklet.js"
      req
    (if (correct-user? req (:userid (:params req)))
      (if (:url (:params req))
        (let [bm-params (select-keys (:params req) [:url :userid :category :title])
              bookmark (bm-model/create! (set/rename-keys bm-params {:userid :user-id
                                                                     :category :category-id}))]
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

(defroutes boilerpipe-routes
  (GET "/plain-text"
       {{:keys [url]} :params :as req}
       (try-user req
         (if url
           (bp-views/boilerpipe-view user url)
           (bp-views/boilerpipe-form-view user)))))

(defroutes app-routes
  slow-route
  public-routes
  boilerpipe-routes
  (context ["/users/:user-id" :user-id #"[0-9]+" :cat-id #"[0-9]+"] req
    create-bookmark
    (friend/wrap-authorize private-user-routes #{::user-model/user})
    (friend/wrap-authorize private-bookmark-routes #{::user-model/user})
    (friend/wrap-authorize private-category-routes #{::user-model/user}))
  bookmarklet-route
  (friend/logout (ANY "/logout" req (ring.util.response/redirect "/")))
  (route/files "/" {:root "resources"}))


(defn original-url
  "Build a uri from a request map, taken from somewhere I can't remember"
  [{:keys [scheme server-name server-port uri query-string]}]
  (str (name scheme) "://" server-name
       (cond
        (and (= :http scheme) (not= server-port 80)) (str \: server-port)
        (and (= :https scheme) (not= server-port 443)) (str \: server-port)
        :else nil)
       uri
       (when (seq query-string)
         (str \? query-string))))

(defn unauthorized-handler [& args]
  {:status 401
   :body "Access denied."})

(defonce app
  (-> #'app-routes
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn user-model/credentials)
                            :workflows [#'custom-workflows/registration
                                        #'custom-workflows/login] 
                            :login-uri "/login"
                            :unauthorized-handler
                            #'unauthorized-handler})
      handler/site))

(def server (atom nil))

(defn stop-server []
  (when @server
    (.stop @server)))

(defn start-server []
  (if @server
    (println "Server already running")
    (reset! server (ring-server/serve #'app {:port 3000 
                                             :join? false
                                             :open-browser? false}))))
(defn -main []
  (println "env: " (e/env :test))
  (start-server))
