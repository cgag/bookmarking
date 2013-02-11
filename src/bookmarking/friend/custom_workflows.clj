(ns bookmarking.friend.custom-workflows
  (:require [bookmarking.models.user :as user]
            [bookmarking.models.bookmark :as bm-model]
            [clojure.string :as s]
            [clojure.set :as set]
            [cemerick.friend.workflows :as workflow]
            [cemerick.friend :as friend]
            [bookmarking.views.home :as h]
            [hiccup.core :refer :all]
            [compojure.response :as resp]
            [bookmarking.views.util :as util]))


;;;; TODO: (if (seq errors))
(defn registration [{:keys [uri request-method params] :as req}]
  (when (and (= "/users" uri)
             (= :post request-method))
    (let [{:keys [user errors]} (user/create! params)]
      (if-not (empty? errors)
        (condp = (:form params)
          "home"     (resp/render (h/home nil {:errors errors :params params}) {})
          "register" (resp/render (h/register nil {:errors errors :params params}) {})
          (resp/render (h/register nil errors) {}))
        (let [user-creds (user/cred-map user)] 
          (workflow/make-auth user-creds
                              {::friend/redirect-on-auth?
                               (str "/users/" (:identity user-creds))}))))))

(defn make-user-auth [user & [opts]]
  (workflow/make-auth user
                      (merge 
                       {::friend/workflow :interactive-form
                        ::friend/redirect-on-auth? (str "/users/" (:identity user))}
                       opts)))

;; TODO: Desperately needs refactoring
;; Couldn't figure out how to redirect to user's bookmarks using the interactive-form workflow
;; TODO: make sure username and password aren't blank/nil. (maybe move this to the credential function
;; and have that return a user like when one is created, or errors
;; TODO: thing ref-req will never be nil, would be "nil"
;;

                                        ;(if-not ref-req
                                        ;(make-user-auth user)
                                        ;(when (and (= :post (:request-method ref-req))
                                        ;(re-find #"/users/[0-9]+/bookmarks" (:uri ref-req)))
                                        ;(if-not (= (:id user) (Integer. (:user-id (:params ref-req))))
                                        ;(make-user-auth user)
                                        ;(let [bookmark (bm-model/create! (:params ref-req))]
                                        ;(if (:errors bookmark)
                                        ;(resp/render "ERROR" {})
                                        ;(make-user-auth user))))))

(defn login [{:keys [uri request-method params] :as request}]
  (when (and (= "/" uri)
             (= :post request-method))
    (let [{:keys [username password] :as creds} (select-keys params [:username :password])
          ref-req (when (:ref-req params) (read-string (util/de-entify (:ref-req params))))
          get-params (when (:get-params params) (read-string (util/de-entify (:get-params params))))]
      (if-let [user (and username password
                         ((:credential-fn (::friend/auth-config request))
                          (with-meta creds {::friend/workflow ::custom-login})))]
        (if-not ref-req
          ;; TODO: need to verify user-id from get-params == the newly logged in user-id
          (if (and get-params
                   (= (Integer. (:userid get-params))
                      (:id user)))
            (let [bm-params (set/rename-keys get-params {:userid :user-id
                                                         :category :category-id})
                  bookmark (bm-model/create! bm-params)]
              (if (:erorrs bookmark)
                (resp/render "ERROR" {})
                (make-user-auth user {::friend/redirect-on-auth? (:url get-params)})))
            (make-user-auth user))
          (when (and (= :post (:request-method ref-req))
                     (re-find #"/users/[0-9]+/bookmarks" (:uri ref-req)))
            (if-not (= (:id user) (Integer. (:user-id (:params ref-req))))
              (make-user-auth user)
              (let [bookmark (bm-model/create! (:params ref-req))]
                (if (:errors bookmark)
                  (resp/render "ERROR" {})
                  (make-user-auth user))))))
        (let [errors {:login #{"failed."}}
              errors (if (s/blank? username)
                       (merge errors {:username #{"can't be blank"}})
                       errors)
              errors (if (s/blank? password)
                       (merge errors {:password #{"can't be blank"}})
                       errors)]
          (condp = (:form params)
            "home"  (resp/render (h/home  nil {:errors errors :params params :ref-req ref-req}) {})
                                        ;"login" (resp/render (h/login nil {:errors errors :params params :ref-req ref-req}) {})
            "login" (ring.util.response/redirect (str "/login?" (util/query-str get-params)))
            (resp/render  (h/login nil {:errors errors :params params :ref-req ref-req}) {})))))))
