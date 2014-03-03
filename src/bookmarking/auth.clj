(ns bookmarking.auth
  (:require [cemerick.friend :as friend]
            [bookmarking.models.user :as user-model]))

(defn valid-id?  
  "Return true if the user id value from the request/url is equal to the 
  currently authenticated user's identity, or the current user is an admin"
  [param-id identity]
  (let [current-id (:identity (friend/current-authentication identity))]
    (or (friend/authorized? #{::user-model/admin} identity)
        (and (friend/authorized? #{::user-model/user} identity)
             (= (Long/parseLong param-id) current-id)))))

(defmacro try-user [req & body]
  `(let [~'user (-> ~req
                    friend/identity
                    friend/current-authentication)]
     ~@body))

;; TODO: take just the req as a paramater?
(defmacro authorized-user
  "Ensures the body is only executed if the current authenticated user's identity
  is equal to the given user id (taken from the url)
  Ex:
  (GET  \"/:id\" [id :as req]
    (authorized-user id req
      (users/show req)))
  "
  [id req & body]
  `(let [identity# (friend/identity ~req)]
     (if-not (valid-id? ~id identity#)
       (friend/throw-unauthorized identity# {})
       (try-user ~req
                 ~@body))))

(defn correct-user?
  "True if currently logged in user has id of user-id"
  [req user-id]
  (try-user req
            (and user (valid-id? user-id (friend/identity req)))))
