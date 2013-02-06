(ns bookmarking.views.bookmarks
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.views.util :refer [error-list]]
            [bookmarking.models.url :as url]
            [clojure.string :as s]
            [cemerick.url :as cu]
            [hiccup.core :refer :all]
            [hiccup.element :refer :all]
            [hiccup.form :refer [form-to text-field hidden-field
                                 submit-button label]])
  (:import [java.net URLEncoder]))

(declare new-bookmark-form edit-bookmark-form)

(defn new [user & [bookmark]]
  (main-layout user "new bookmark"
    (new-bookmark-form (:id user) bookmark)))

(defn edit [user cat-id url-id & [errors]]
  (main-layout user "Edit bookmark"
               (edit-bookmark-form user cat-id url-id errors)))

(defn all [id]
  (html [:p (str "User " id "'s bookmarks")]))

(defn show [user-id url-id]
  (str "user-id: " user-id " url-id " url-id))


(defn host [url]
  (let [host (:host (cu/url url))]
    (if (.startsWith host "www.")
      (subs host 4)
      host)))

(defn bookmark-fields [{:keys [title url category]}]
  ;; hiccup wants seqs of elements, vector doesn't work
  (seq [[:li (label "url" "Url")
         (text-field "url" url)]
        [:li (label "category" "Category")
         (text-field "category" category)]
        [:li (label "title" "Title (optional, will default to the page's actual title)")
         (text-field "title" title)]
        [:li (label "submit" "")
         (submit-button "Submit")]]))

(defn edit-bookmark-form [user cat-id url-id & [{:keys [errors] :as bm}]]
  (do
    (println "user: " user)
    (println "form target: " (str "/users" (:id user) "/bookmarks/" url-id))
    [:div#edit-bookmark-wrapper
     (when errors (error-list errors))
     [:ul#edit-bookmark-form
      (form-to [:post (str "/users/" (:id user) "/bookmarks/" url-id)]
               [:li.hidden (hidden-field "current-cat" cat-id)]
               (bookmark-fields bm))]]))

(defn new-bookmark-form [user-id & [{:keys [errors] :as bm}]]
  [:div#new-bookmark-wrapper
   (when errors
     (error-list errors))
   [:fieldset
    [:ul#new-bookmark-form
     (form-to [:post (str "/users/" user-id "/bookmarks")]
              (bookmark-fields bm))]]])

(defn my-encode
  "handle stupid fucking hashbangs, there has to be a better way"
  [url]
  (-> url
      cu/url
      (update-in [:anchor] cu/url-encode)
      str))

(defn display-bookmark [bookmark]
  (let [{:keys [user_id url_id description title category_id]} bookmark
        url (my-encode (:url (url/by-id url_id)))
        host (host url)]
    [:div.bookmark-wrapper
     [:div.bookmark-title (link-to {:class "bookmark"} url title)
      [:div.delete (link-to {:class "delete-bookmark"
                             :title "Delete Bookmark"}
                            (str "/users/" user_id "/categories/" category_id
                                 "/bookmarks/" url_id "/delete")
                            "&#10006;")]]
     [:div.bookmark-host {:title url}
      (if (s/blank? title)
        (link-to url host)
        host)
      [:div.bm-links [:ul
                      [:li (link-to (str "/users/" user_id "/categories/" category_id
                                         "/bookmarks/" url_id "/edit")
                                    "edit")]
                      [:li (link-to (str "/plain-text?url=" url) "plain text")]]]]]))

(defn search-form [user-id cat-id]
  (let [search-path (str "/users/" user-id "/categories/" cat-id "/search")]
    [:div#search-bookmarks-wrapper
     [:ul#search-bookmarks
      (form-to [:post search-path]
               [:li (label "search" "Search bookmarks:")]
               [:li (text-field {:placehold "E.g. lonely planet"} "query" "")])]]))


