(ns bookmarking.views.bookmarks
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.views.util :refer [error-list]]
            [bookmarking.models.url :as url]
            [clojure.string :as s]
            [cemerick.url :as cu]
            [hiccup.core :refer :all]
            [hiccup.element :refer :all]
            [hiccup.form :refer [form-to text-field 
                                 submit-button label]]))

(declare new-bookmark-form)

(defn new [user-id & [bookmark]]
  (main-layout nil "new bookmark"
    (new-bookmark-form user-id bookmark)))

;; TODO: handle errors. separate param or key in map?
;; TODO: make sure this is secure from people posting to a different
;; user id

(defn all [id]
  (html [:p (str "User " id "'s bookmarks")]))

(defn show [user-id bookmark-id]
  (str "user-id: " user-id " bookmark-id " bookmark-id))

(defn edit [user-id bookmark-id]
  (html [:p (str "Edit page for user-id: " user-id
                 " bookmark-id: " bookmark-id)]))

(defn update! [req])

(defn host [url]
  (let [host (:host (cu/url url))]
    (if (.startsWith host "www.")
      (subs host 4)
      host)))

;; TODO: host and url are the same, this needs reorganized
(defn display-bookmark [bookmark]
  (let [{:keys [user_id url_id description title category_id]} bookmark
        url (:url (url/by-id url_id))
        host (host url)]
    [:div {:class "bookmark-wrapper"}
     [:div.controls (link-to {:class "delete-bookmark"
                              :title "Delete Bookmark"}
                             (str "/users/" user_id 
                                  "/bookmarks/" url_id "/delete?category=" category_id)
                             "&#10006;")]
     [:div.bookmark-title (link-to {:class "bookmark"} url title)]
     [:div {:class "bookmark-description"} description]
     [:div {:class "bookmark-host" :title url} (if (s/blank? title)
                                                 (link-to url host)
                                                 host)]]))

(defn new-bookmark-form [user-id & [{:keys [errors title url category]}]]
  [:div#new-bookmark-wrapper
   (when errors
     (error-list errors))
   [:fieldset
    [:ul#new-bookmark-form
     (form-to [:post (str "/users/" user-id "/bookmarks")]
              [:li (label "url" "Url")
                   (text-field "url" url)]
              [:li (label "category" "Category")
                   (text-field "category" category)]
              [:li (label "title" "Title (optional, will default to the page's actual title)")
                   (text-field "title" title)]
              [:li (label "submit" "")
                   (submit-button "submit")])]]])
