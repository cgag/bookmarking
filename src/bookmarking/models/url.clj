(ns bookmarking.models.url
  (:require [korma.core :refer :all]
            [bookmarking.models.entities :as entities]
            [validateur.validation :refer [presence-of validation-set length-of]]))

;; TODO: SECURITY: Possible concern: the user types javascript into the url, 
;; then saves the url, need to ensure the javascript won't be executed 
;; when attempting to display the url in the list of bookmarks

(declare validate-url by-url)

;; TODO: title should be part of bookmark model? 
;; TODO: not sure checking if it already exists should be done
;; in this function

(defn absolute-url
  "Ensure a url starts with http://"
  [url]
  (if-not (re-find #"^http(s)?://" "http://www.butts.com")
    (str "http://" url)
    url))

(defn url-params [params]
  (-> params
    (select-keys [:url])
    (update-in [:url] absolute-url)))

;; TODO: better way to determine if already exists
;; TODO: shoudl all create functions have this behavior of returning the alredy existing one?  Should any?
(defn create! [params]
  (println "in create url with params: " params)
  (let [uparams (url-params params)
        errors  (validate-url uparams)]
    (if-not (empty? errors)
      {:errors errors}
      (insert entities/urls
              (values uparams)))))

(defn by-url [url]
  (first
    (select entities/urls
            (where {:url (absolute-url url)}))))

(defn by-id [id]
  (first
    (select entities/urls
            (where {:id id}))))


;; validations

(defn unique-url [m]
  (if (by-url (:url m))
    [false {:url #{"already exists."}}]
    [true  {}]))

(def validate-url (validation-set
                    unique-url
                    (presence-of :url)
                    (length-of :url :within (range 1 3000) 
                               :allow-blank false)))
