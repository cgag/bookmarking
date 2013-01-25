(ns bookmarking.models.bookmark
  (:refer-clojure :exclude [count])
  (:require [korma.core :refer [select modifier where insert
                                aggregate values fields join
                                delete dry-run order]]
            [clojure.string :as s]
            [clojure.set :as set]
            [bookmarking.models.entities :as entities]
            [bookmarking.models.url :as url]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of]]))

;;TODO: should this model only be for validations?  What should getting a 
;; user's bookmarks look like?
;; (user/bookmarks user-id) or (bookmark/bookmarks user-id)
;; How should has-one/has-many relationships be modeled?

(declare validate-bookmark)

(defn bookmark-params [params]
  (let [bm-params (-> params
                      (select-keys [:user-id :category :title])
                      (set/rename-keys {:user-id :user_id})
                      (update-in [:user_id] #(Integer. %)))]
    (if (s/blank? (:category bm-params))
      (dissoc bm-params :category)
      bm-params)))

;; TODO: stop returning a separate error map, just have an error key in the url map
;; -- same for bookmarks and users
;; TODO: handle url errors
;; TODO: don't allow blank urls
;; TODO: Don't write url to db if the bookmark is going to fail?
;; TODO: handle url already existing, same for all models?
;; TODO: maybe validation shouldn't occur in the create function, almost certainly shouldn't in fact
(defn create! [params]
  (let [url-params (select-keys params [:url])
        url (or (url/by-url (:url url-params)) 
                (url/create! url-params))] 
    (if-not (empty? (:errors url))
      {:errors (:errors url)}
      (let [bm-params (bookmark-params params)
            bookmark (assoc bm-params :url_id (:id url)) 
            errors (validate-bookmark bookmark)] 
        (if-not (empty? errors)
          {:errors errors}
          {:bookmark (insert entities/bookmarks
                             (values bookmark))})))))

(defn by-id [id]
  (select entities/bookmarks
          (where {:id id})))

;; TODO: I shouldn't ahve to specify clojure.core/or here should I?  Or am I just really doing things wrong
;; TODO: should this be in the user model?
;; TODO: The bookmarks are unordered, should list by time saved. Which means we need to actually store time created
(defn bookmarks [user-id & [{:keys [category]}]]
  (let [category (or category "default")] 
    (select entities/bookmarks
            (where {:users.id (Integer. user-id)
                    :category category})
            (join entities/users {:bookmarks.user_id :users.id})
            (join entities/urls  {:bookmarks.url_id :urls.id})
            (order :created_at :DESC))))

(defn categories [user-id]
  (->> (select entities/bookmarks
               (modifier "distinct")
               (fields :category)
               (where {:users.id (Integer. user-id)})
               (join entities/users {:bookmarks.user_id :users.id})
               (order :category))
       (map :category)))

(defn count [user-id]
  (:count (first 
           (select entities/bookmarks
                   (aggregate (count :*) :count) 
                   (where {:users.id (Integer. user-id)})
                   (join entities/users {:bookmarks.user_id :users.id})
                   (join entities/urls  {:bookmarks.url_id  :urls.id})))))

;; TODO: user a map instead of positional args
(defn find-bookmark [user-id url-id & [category]]
  (let [category (or category "default")]
    (first
     (select entities/bookmarks
             (where {:url_id  (Integer. url-id)
                     :user_id (Integer. user-id)
                     :category category})))))

(defn delete! [user-id url-id]
  (delete entities/bookmarks
          (where {:user_id (Integer. user-id)
                  :url_id  (Integer. url-id)})))

;; validations

;; not sure exactly how to go about this.  Need to convert params to nil if blank?
(defn unique-bookmark [bm]
  (let [{:keys [user_id url_id category]} bm
        from-db (find-bookmark user_id url_id category)]
    (if from-db
      [false {:bookmark #{"already exists."}}]
      [true {}])))

(def validate-bookmark (validation-set
                        (presence-of :url_id)
                        (presence-of :user_id)
                        (numericality-of :url_id)
                        (numericality-of :user_id)
                        unique-bookmark))
