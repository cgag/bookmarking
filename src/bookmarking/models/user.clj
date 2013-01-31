(ns bookmarking.models.user
  (:require [clojure.string :as s]
            [korma.core :refer :all]
            [bookmarking.views.util :as util]
            [bookmarking.models.entities :as entities]
            [validateur.validation :refer [validation-set presence-of 
                                           valid? invalid? length-of]]
            [cemerick.friend.credentials :as creds]
            [compojure.response :as resp]))


(declare confirm-password optional valid-email
         validate-user validate-update has-password?
         update-map email-errors credentials)

(defn create! [params]
  (let [user-map (select-keys params [:username :password 
                                      :password_confirmation :email])
        errors   (validate-user user-map)]
    (if-not (empty? errors)
      {:errors errors}
      {:user (insert entities/users
                     (values (-> user-map
                               (dissoc    :password_confirmation)
                               (update-in [:password] creds/hash-bcrypt))))})))

(defn update! [user params]
  (let [params (util/remove-blanks params)
        {:keys [current-pass]} params
        updated-attributes (update-map params)]
    (cond
     (nil? current-pass)
     {:errors {:current-password #{"can't be blank."}}}

     (not (has-password? user current-pass))
     {:errors {:current-password #{"incorrect."}}}

     (:errors updated-attributes)
     updated-attributes

     :else
     (update entities/users
             (where {:id(:id user)})
             (set-fields (merge {:updated_at (sqlfn now)}
                                (update-map params)))))))

(defn update-map [params]
  (let [params (util/remove-blanks params)
        {:keys [email new-pass new-pass-conf]} params
        updated-attributes (merge {}
                                  (when email {:email email})
                                  (when (and new-pass (= new-pass new-pass-conf))
                                    {:password (creds/hash-bcrypt new-pass)}))]
    (cond
     (email-errors updated-attributes) {:errors (email-errors updated-attributes)}
     (empty? updated-attributes)       {:errors {:nothing #{"to update"}}}
     :else updated-attributes)))

(defn email-errors [m]
  (when-let [errors (and (:email m)
                         ((valid-email :email) {:email (:email m)}))]
    (if (seq (second errors))
      (second errors)
      nil)))

(defn has-password? [user password]
  (boolean
   (creds/bcrypt-credential-fn credentials {:username (:username user)
                                            :password password})))

(defn username [id]
  (-> (select entities/users
              (fields :username)
              (where {:id (Integer. id)}))
    first
    :username))

(defn delete-by-id! [id]
  (delete entities/users
          (where {:id (Integer. id)})))

(defn delete-by-username! [username]
  (delete entities/users
          (where {:username username})))

;; TODO: add index on username (and id? is that already done by default?)
(defn by-username [username]
  (first (select entities/users
                 (where {:username username}))))

(defn by-id [id]
  (first (select entities/users
                 (where {:id (Integer. id)}))))

(defn all []
  (select entities/users))

(declare cred-map)

;; *ns* gets bound to clojure.core when this is evaluted?
(defn credentials
  "Given a username, return that users credentials or nil if the user
  doesn't exist."
  [username]
  (when-let [user (by-username username)]
    (cred-map user)))

(defn cred-map [user]
  {:username (:username user)
   :identity (:id user)
   :id       (:id user)
   :roles    (hash-set (keyword "bookmarking.models.user" (:role user)))
   :password (:password user)})

;; validations

(def email-regex #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(defn optional
  [attribute validation-fn]
  (let [get-f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (get-f m attribute)]
        (if-not (or (nil? v) (empty? v) (s/blank? v)) 
          ((validation-fn attribute) m)
          [true {}])))))

(defn valid-email 
  [attribute & {:keys [message] :or {message "address invalid."}}]
  (let [get-f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [email  (get-f m attribute)
            errors (if (re-find email-regex email)
                     {}
                     {attribute #{message}})]
        [(empty? errors) errors])))) 

;; TODO: use clojure.string/blank? instead of empty?
(defn confirm-password [attr-a attr-b]
  (fn [m]
    (let [a (get m attr-a)
          b (get m attr-b)]
      (cond
        (empty? a) [false {attr-a #{"can't be blank."}}]
        (empty? b) [false {attr-b #{"can't be blank."}}]
        (not= a b) [false {attr-a #{"not equal to password confirmation."}}]
        :else [true {}]))))

(defn valid-username [name-key]
  (fn [m]
    (let [username (get m name-key)]
      (cond
        (s/blank? username) [false {name-key #{"cannot be blank."}}]
        (by-username username) [false {name-key #{"already taken."}}]
        :default [true {}]))))

(def validate-user (validation-set
                     (presence-of :username)
                     (length-of :username :within (range 1 31) :allow-blank false)
                     (confirm-password :password :password_confirmation)
                     (valid-username :username)
                     (optional :email valid-email)))

(def validate-update (validation-set
                      (presence-of :user-id)
                      (optional :email valid-email)))

;; (update entities/users
;;               (where {:id 1})
;;               (set-fields {:email nil
;;                            :updated_at (sqlfn now)}))
