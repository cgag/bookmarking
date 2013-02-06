(ns bookmarking.views.bookmarks
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.views.util :refer [error-list user-link]]
            [bookmarking.models.url :as url]
            [bookmarking.models.bookmark :as bm]
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

(defn search-form [user-id cat-id & [query]]
  (let [search-path (str "/users/" user-id "/categories/" cat-id "/search")]
    [:div#search-bookmarks-wrapper
     [:ul#search-bookmarks
      (form-to [:post search-path]
               [:li (text-field {:placehold "E.g. lonely planet"} "query" query)]
               [:li (submit-button "Search")])]]))


(defn page-links [page num-pages]
  (let [page (Integer. page)
        num-pages (Integer. num-pages)
        page (if (> page num-pages) num-pages page)
        has-prev? (fn [p] (> p 1))
        has-next? (fn [p] (< p num-pages))]
    (seq [(if (has-prev? page)
            (link-to (str "?page=" (dec page)) "<-")
            "<-")
          " " page " "
          (if (has-next? page)
            (link-to (str "?page=" (inc page)) "->")
            "->")])))

(declare bookmark-list bookmark-section)

(defn display-bookmarks [user-id cat-id bookmarks {:keys [page per-page]}]
  (let [num-pages (bm/num-pages user-id cat-id per-page)
        per-page 50]
    [:div#bookmarks
     [:div#add-new-bookmark [:h4 (user-link user-id "/bookmarks/new" "Add bookmark")]]
     [:div#pagination (page-links page num-pages)]
     [:div#bookmarks  (bookmark-list bookmarks)]]))


(defn bookmark-list [bookmarks]
  [:div.bookmark-list
   (let [bm-list (for [bm bookmarks]
                   (display-bookmark bm))]
     (if (seq bm-list)
       bm-list
       [:p "No bookmarks yet for this category."]))])
