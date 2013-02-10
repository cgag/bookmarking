(ns bookmarking.views.users
  (:require [hiccup.core :refer :all]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer :all]
            [environ.core :as e]
            [bookmarking.views.util :refer [user-link error-list]]
            [bookmarking.models.user :as user]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.models.category :as cat-model]
            [bookmarking.views.bookmarks :as bm-views]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [cemerick.friend :as friend])
  (:import  java.net.URLEncoder))


(declare category-list bookmarklet display-categories
         bookmarks-section categories-section bookmarklet-section)

(defn show [user cat-id & [{:keys [page] :or {page 1}}]]
  (let [[user-id cat-id page] [(Integer. (:id user)) (Integer. cat-id) (Integer. page)]
        cat-name (cat-model/name cat-id)
        user-id  (:id user)
        per-page 50
        [bookmarks total-bms] (bm-model/bookmarks user-id cat-id {:page page :per-page per-page})
        num-pages (bm-views/num-pages total-bms per-page)]
    (main-layout user (str (:username user) "'s stuff") 
      [:div.row.dongs
       [:div.span12.butts
        (bm-views/bookmark-pagination-links page num-pages)]]
      [:div.row
       [:div.span9
        [:div.bookmark-section-left
         (bm-views/bookmarks-section user-id cat-id bookmarks {:page page :per-page per-page})]]
       [:div.span3
        [:div.bookmark-section-right
         (categories-section user-id cat-id)
         [:hr]
         (bookmarklet-section user-id cat-id)]]])))


(defn search-results [user cat-id {:keys [query page per-page]}]
  (let [user-id (:id user)
        page (or page 1)
        per-page (or per-page 50)
        query-str (str "?query=" (URLEncoder/encode query))
        [results num-results] (bm-model/search user-id cat-id query {:page page})]
    (main-layout user (str "Search results for: " query)
      [:div.row
       [:div.span12
        [:div.pagination
         (bm-views/page-links (str query-str "&page=") page (bm-views/num-pages num-results per-page))]]]
      [:div.row
       [:div.span9
        (bm-views/bookmarks-section user-id cat-id results {:page page :query query})]
       [:div.span3
        [:div.right-col
         (categories-section user-id cat-id (fn [uid cid]
                                              (str "/users/" uid "/categories/"
                                                   cid "/search" query-str)))
         (bookmarklet-section user-id cat-id)]]])))

(defn bookmarklet-section [user-id cat-id]
  (let [cat-name (cat-model/name cat-id)]
    [:div#bookmarklets 
     [:h4.bookmarklet "Bookmarklet"]
     [:span.icon-question-sign {:title "Drag this to your bookmarks bar, then click it while on another site to bookmark that site."}]
     [:div.bookmarklet
      [:span.label [:a.bookmarklet {:href (bookmarklet user-id cat-id)} cat-name]]]]))

(defn categories-section [user-id cat-id & [url-fn]]
  [:div.categories 
   [:h4 "Categories"]
   (category-list user-id cat-id url-fn)
   (user-link user-id "/categories/new" "Add Category")
   [:br]
   (user-link user-id "/categories" "Manage Categories")])

(defn bookmarklet-list [user-id]
  (for [category (cat-model/categories user-id)
        :let [cat-id (:category_id category)
              cat-name (:category category)]]
    [:div.bookmarklet
     [:span.label
      [:a.bookmarklet {:href (bookmarklet user-id cat-id)} cat-name]]]))


(defn edit [user & [errors]]
  (main-layout user "Change your password"
    (error-list errors)
    [:div.edit-user-wrapper
     [:fieldset
      [:legend "Edit Account"]
      [:ul.edit-user-form
       (form-to [:post (str "/users/" (:id user))]
                [:li (label "current-pass" "Current Password (required)")]
                [:li (text-field {:placeholder "Current Password"} "current-pass")]
                [:li (label "email" "Email")]
                [:li (text-field {:placeholder (or (:email user) "Email")} "email")]
                [:li (label "New Password" "New Password")]
                [:li (text-field {:placeholder "New Passwword"} "new-pass")]
                [:li (label "new-pass-conf" "Confirm New Password")]
                [:li (text-field {:placeholder "Confirm New Password"} "new-pass-conf")]
                [:li (submit-button {:class "btn btn-large btn-primary"} "Save changes")])]]]))



(defn category-list [user-id current-cat & [url-fn]]
  (let [current-cat (Integer. current-cat)
        url-fn (or url-fn
                   (fn [uid cid] (str "/users/" uid "/categories/" cid)))]
    [:ul
     (for [category (cat-model/categories user-id)
           :let [cat-name (:category category)
                 cat-id   (Integer. (:category_id category))]]
       (link-to (url-fn user-id cat-id)
                [:li (when (= cat-id current-cat)
                       {:class "current-category"}) 
                 cat-name]))]))


(defn bookmarklet [user-id category-id]
  (let [host (e/env :bm-host)
        port (e/env :bm-port)
        ssl-port (e/env :bm-ssl-port)]
    (str "javascript:(function(){
         var newScript = document.createElement('scr' + 'ipt');
         var url = encodeURIComponent(document.location.href);
         var title = encodeURIComponent(document.title);
         var category = encodeURIComponent('" category-id "');"
         "var userid = encodeURIComponent('" user-id "');"
         "var scheme = document.location.protocol;"
         "var port = (scheme === 'http:') ? '" port "' : '" ssl-port "';"
         "newScript.type='text/javascript';"
         "newScript.src= scheme +  '//" host ":' + port + '/js/bookmarklet.js?dummy=' + Math.random() + '&url=' + url + '&title=' + title + '&category=' + category + '&userid=' + userid;"
         "document.body.appendChild(newScript);}())")))
