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
            [cemerick.friend :as friend]))


(declare bookmark-list category-list bookmarklet)

(defn page-links [page num-pages]
  (let [page (Integer. page)
        has-prev? (fn [p] (> p 1))
        has-next? (fn [p] (< p num-pages))]
    (seq [(if (has-prev? page)
            (link-to (str "?page=" (dec page)) "<-")
            "<-")
          " " page " "
          (if (has-next? page)
            (link-to (str "?page=" (inc page)) "->")
            "->")])))

(defn num-pages [user-id category-id per-page]
  (let [pages (int (Math/ceil (/ (bm-model/count user-id category-id) per-page)))]
    (if (= pages 0)
      1
      pages)))

(defn show [user category-id & [{:keys [page] :or {page 1}}]]
  (let [[user-id category-id page] [(Integer. (:id user)) (Integer. category-id) (Integer. page)]
        cat-name (cat-model/name category-id)
        user-id  (:id user)
        per-page 50
        num-pages (num-pages user-id category-id per-page)
        page (if (> page num-pages) num-pages page)]
    (main-layout user (str (:username user) "'s stuff") 
      [:div.container-fluid
       [:div.row-fluid
        [:div.span10
         [:div#add-new-bookmark [:h4 (user-link user "/bookmarks/new" "Add bookmark")]]
         [:div#pagination (page-links page num-pages)]
         [:div#bookmarks  (bookmark-list user-id category-id {:page page :per-page per-page})]]
        [:div.span2
         [:div#categories 
          [:h3 "Categories"]
          (category-list user-id category-id)
          (user-link user "/categories/new" "Add Category")
          [:br]
          (user-link user "/categories" "Manage Categories")]
         [:div#bookmarklets 
          [:h4.bookmarklet "Bookmarklet"]
          [:span.icon-question-sign {:title "Drag this to your bookmarks bar, then click it while on another site to bookmark that site."}]
          [:div.bookmarklet
           [:span.label [:a.bookmarklet {:href (bookmarklet user-id category-id)} cat-name]]]]]]])))


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
    [:div#edit-user-wrapper
     [:fieldset
      [:legend "Password"]
      [:ul#edit-user-form
       (form-to [:post (str "/users/" (:id user))]
                [:li (text-field {:placeholder "Current Password"} "current-pass") "(required)"]
                [:li (text-field {:placeholder (or (:email user) "Email")} "email")]
                [:li (text-field {:placeholder "New Passwword"} "new-pass")]
                [:li (text-field {:placeholder "Confirm New Password"} "new-pass-conf")]
                [:li (submit-button {:class "btn btn-large btn-primary"} "Save changes")])]]]))


(defn bookmark-list [user-id category-id & [page-info]]
  [:div.bookmark-list
   (let [bookmarks (bm-model/bookmarks user-id category-id page-info)
         bm-list (for [bm bookmarks]
                   (bm-views/display-bookmark bm))]
     (if (seq bm-list)
       bm-list
       [:p "No bookmarks yet for this category."]))])


(defn category-list [user-id current-cat]
  (for [category (cat-model/categories user-id)
        :let [cat-name (:category category)
              cat-id   (:category_id category)]]
    [:li (when (= cat-id current-cat)
           {:class "current-category"}) 
     (link-to (str "/users/" user-id "/categories/" cat-id) cat-name)]))


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
