(ns bookmarking.views.bookmarks
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.views.util :refer [error-list user-link encode-url]]
            [bookmarking.models.url :as url]
            [bookmarking.models.bookmark :as bm]
            [bookmarking.models.category :as cat]
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

(defn bookmark-fields [{:keys [title url_id category_id]}]
  (let [url      (:url (url/by-id url_id))
        category (:category (cat/by-id category_id))]
    (seq [[:li (label "url" "Url")
           (text-field "url" url)]
          [:li (label "category" "Category")
           (text-field "category" category)]
          [:li (label "title" "Title (optional, will default to the page's actual title)")
           (text-field "title" title)]
          [:li (label "submit" "")
           (submit-button "Submit")]])))

(defn edit-bookmark-form [user cat-id url-id & [{:keys [errors]}]]
  (let [user-id (:id user)
        current (bm/find-bookmark user-id url-id cat-id)]
    [:div.edit-bookmark-wrapper
     (when errors (error-list errors))
     [:ul#edit-bookmark-form
      (form-to [:post (str "/users/" user-id "/bookmarks/" url-id)]
               [:li.hidden (hidden-field "current-cat" cat-id)]
               (bookmark-fields current))]]))

(defn new-bookmark-form [user-id & [{:keys [errors] :as bm}]]
  [:div.new-bookmark-wrapper
   (when errors
     (error-list errors))
   [:fieldset
    [:ul.new-bookmark-form
     (form-to [:post (str "/users/" user-id "/bookmarks")]
              (bookmark-fields bm))]]])


(defn display-bookmark [bookmark]
  (let [{:keys [user_id url_id description title category_id]} bookmark
        url (encode-url (:url (url/by-id url_id)))
        host (host url)]
    [:div.bookmark
     [:div.title (link-to {:class "link"} url title)
      [:div.delete (link-to {:class "delete"
                             :title "Delete Bookmark"}
                            (str "/users/" user_id "/categories/" category_id
                                 "/bookmarks/" url_id "/delete")
                            "&#10006;")]]
     [:div.host {:title url}
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
    [:div.search-bookmarks-wrapper
     [:ul.search-bookmarks
      (form-to [:get search-path]
               [:li (text-field {:placehold "E.g. lonely planet"} "query" query)]
               [:li (submit-button "Search")])]]))

(defn num-pages [total per-page]
  (let [pages (int (Math/ceil (/ total per-page)))]
    (if (= pages 0)
      1
      pages)))

(defn prange [p page-range num-pages]
  {:pre [(pos? num-pages)]}
  (let [page-ranges (partition page-range 1 (range 1 (inc num-pages)))
        midpoint (Math/ceil (/ page-range 2))]
    (nth page-ranges
        (dec (min (Math/ceil (/ p midpoint))
                  (count page-ranges))))))

(defn page-links [base-url page num-pages]
  (let [page (Integer. page)
        num-pages (Integer. num-pages)
        page-range 5 ; show 5 page links 
        page (min page num-pages)
        has-prev? (> page 1)
        has-next? (< page num-pages)
        page-window (if (< num-pages page-range)
                      (range 1 (inc num-pages))
                      (prange page page-range num-pages))]
    [:ul
     [:li (when-not has-prev? {:class "disabled"})
      (if has-prev?
        (link-to (str base-url (dec page)) "«")
        [:span "«"])]
     (for [p page-window]
       (if (== p page)
         [:li.active [:span p]]
         [:li (link-to (str base-url p) p)]))
     [:li (when-not has-next? {:class "disabled"})
      (if has-next?
        (link-to (str base-url (inc page)) "»")
        [:span "»"])]]))

(def bookmark-pagination-links  (partial page-links "?page="))

(declare bookmark-list bookmark-section)

(defn bookmarks-section [user-id cat-id bookmarks {:keys [query page per-page]}]
  (let [per-page (or per-page 50)
        num-pages (num-pages (count bookmarks) per-page)]
    [:div.bookmarks-wrapper
     [:div.controls
      [:div.add-new-bookmark
       [:p (user-link user-id "/bookmarks/new" "Add bookmark")]]
      (search-form user-id cat-id query)]
     [:div.bookmarks  (bookmark-list bookmarks)]]))


(defn bookmark-list [bookmarks]
  [:div.bookmark-list
   (let [bm-list (for [bm bookmarks]
                   (display-bookmark bm))]
     (if (seq bm-list)
       bm-list
       [:p "No bookmarks yet for this category."]))])
