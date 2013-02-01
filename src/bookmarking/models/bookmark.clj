(ns bookmarking.models.bookmark
  (:refer-clojure :exclude [count])
  (:require [korma.core :refer [select modifier where insert
                                aggregate values fields join
                                delete dry-run order update
                                set-fields]]
            [clojure.string :as s]
            [clojure.set :as set]
            [net.cgrand.enlive-html :as enlive]
            [clj-http.client :as http]
            [bookmarking.models.entities :as entities]
            [bookmarking.models.url :as url]
            [bookmarking.models.category :as cat]
            [bookmarking.views.util :refer [remove-blanks
                                            select-field
                                            select-one]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of]]))

;;TODO: should this model only be for validations?  What should getting a 
;; user's bookmarks look like?
;; (user/bookmarks user-id) or (bookmark/bookmarks user-id)
;; How should has-one/has-many relationships be modeled?

(declare validate-bookmark validate-update find-bookmark)

(defn bookmark-params [params]
  (let [bm-params (-> params
                      (select-keys [:user-id :category-id :title])
                      (update-in [:category-id] #(or % 1))
                      (set/rename-keys {:user-id :user_id
                                        :category-id :category_id})
                      (update-in [:user_id] #(Integer. %))
                      (update-in [:category_id] #(Integer. %)))]
    bm-params))


(defn save-title [bm url & [{:keys [block-for]}]]
  (let [ftr (future (let [resp  (:body (http/get url {:conn-timeout 5000
                                                      :socket-timeout 15000}))
                          html  (enlive/html-resource (java.io.StringReader. resp))
                          title (-> (enlive/select html [:title])
                                  first
                                  :content
                                  first)]
                      (update entities/bookmarks
                              (set-fields {:title title})
                              (where (select-keys bm [:user_id :url_id])))))]
    (when block-for
      (deref ftr block-for "Too long"))))

;; TODO: stop returning a separate error map, just have an error key in the url map
;; -- same for bookmarks and users
;; TODO: don't allow blank urls
;; TODO: Don't write url to db if the bookmark is going to fail?
;; TODO: handle url already existing, same for all models?
;; TODO: create a kibit rule for (if-not (empty? x)) -> (if (seq x))
(defn create! [params]
  (let [url-params (select-keys params [:url])
        url (or (url/by-url (:url url-params)) 
                (url/create! url-params))] 
    (if-not (empty? (:errors url))
      {:errors (:errors url)}
      (let [bm-params (bookmark-params params)
            bookmark  (assoc bm-params :url_id (:id url)) 
            errors    (validate-bookmark bookmark)] 
        (if-not (empty? errors)
          {:errors errors}
          (let [new-bm (insert entities/bookmarks
                               (values bookmark))]
            (save-title new-bm (:url url))
            {:bookmark new-bm}))))))

;; TODO: find a cleaner way to do this
(defn update-params [params]
  (let [uparams (-> params 
                    (select-keys [:url :title :category])
                    remove-blanks)
        new-url-id      (when (:url uparams)
                          (:id (or (url/by-url (:url uparams))
                                   (url/create! {:url (:url uparams)}))))
        new-category-id (when (:category uparams)
                          (:category_id (or (cat/by-name (:category uparams))
                                   (cat/create! (:user-id params) (:category params)))))
        uparams (if-not new-url-id
                  uparams
                  (-> uparams
                      (dissoc :url)
                      (assoc :url_id new-url-id)))
        uparams (if-not new-category-id
                  uparams
                  (-> uparams
                      (dissoc :category)
                      (assoc :category_id new-category-id)))]
    uparams))

(defn update! [user-id url-id params]
  (let [current-cat (:current-cat params)
        current-bm (find-bookmark user-id url-id current-cat)
        uparams (update-params params)]
    (update entities/bookmarks
            (where {:user_id (Integer. user-id)
                    :url_id  (Integer. url-id)
                    :category_id (Integer. current-cat)})
            (set-fields (merge current-bm
                               uparams)))))

;; TODO: I shouldn't ahve to specify clojure.core/or here should I?  Or am I just really doing things wrong
;; TODO: should this be in the user model?
;; TODO: The bookmarks are unordered, should list by time saved. Which means we need to actually store time created
(defn bookmarks [user-id & [{:keys [category-id]}]]
  (select entities/bookmarks
          (where (merge
                  {:user_id (Integer. user-id)}
                  (when category-id {:category_id category-id})))
          (order :created_at :DESC)))

(defn categories [user-id]
  (select entities/users-categories
          (fields :category_id :categories.category)
          (where {:user_id (Integer. user-id)})
          (join entities/categories {:category_id :categories.id})
          (order :created_at)))

(defn count [user-id]
  (:count (first 
           (select entities/bookmarks
                   (aggregate (count :*) :count) 
                   (where {:users.id (Integer. user-id)})
                   (join entities/users {:bookmarks.user_id :users.id})
                   (join entities/urls  {:bookmarks.url_id  :urls.id})))))

;; TODO: user a map instead of positional args
(defn find-bookmark [user-id url-id category_id]
  (select-one entities/bookmarks
              (where {:url_id   (Integer. url-id)
                      :user_id  (Integer. user-id)
                      :category_id (Integer. category_id)})))

(defn delete! [user-id url-id category-id]
  (delete entities/bookmarks
          (where {:user_id (Integer. user-id)
                  :url_id  (Integer. url-id)
                  :category_id (Integer. category-id)})))

;; validations

;; not sure exactly how to go about this.  Need to convert params to nil if blank?
(defn unique-bookmark [bm]
  (let [{:keys [user_id url_id category_id]} bm
        from-db (find-bookmark user_id url_id category_id)]
    (if from-db
      [false {:bookmark #{"already exists."}}]
      [true {}])))

(def validate-bookmark (validation-set
                        (presence-of :url_id)
                        (presence-of :user_id)
                        (presence-of :category_id)
                        (numericality-of :url_id)
                        (numericality-of :user_id)
                        (numericality-of :category_id)
                        unique-bookmark))
