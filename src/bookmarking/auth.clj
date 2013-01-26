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

;; TODO: take just the req as a paramater
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

;; TODO: make something like this -- maybe unnecessayr w/ context macro
;(defmacro authorize-routes [id req & routes])

;; TODO: research heirarchies, confirm doing this in this namespace will work
;; for the handler
;(derive ::user-model/admin ::user-model/user)

;; From sandbar.auth
;(declare filter-channel-config find-matching-config)

;(defn ^:dynamic with-secure-channel
  ;"Middleware function to redirect to either a secure or insecure channel."
  ;[handler config port ssl-port]
  ;(fn [request]
    ;(let [ssl-config (filter-channel-config config)
          ;channel-req (find-matching-config ssl-config request)
          ;uri (:uri request)]
      ;(cond (and (= channel-req :ssl) (= (:scheme request) :http))
            ;(redirect-301 (to-https request ssl-port))
            ;(and (= channel-req :nossl) (= (:scheme request) :https))
            ;(redirect-301 (to-http request port))
            ;:else (handler request)))))

;(defn filter-channel-config
  ;"Extract the channel configuration from the security configuration."
  ;[config]
  ;(map #(vector (first %) (if (vector? (last %))
                            ;(last (last %))
                            ;(last %)))
       ;(filter #(cond (and (keyword? (last %))
                           ;(not (role? (last %))))
                      ;true
                      ;(and (vector? (last %))
                           ;(not (role? (last (last %)))))
                      ;true
                      ;:else false)
               ;(partition 2 config))))
