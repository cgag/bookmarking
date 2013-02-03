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

(defn show [user category-id]
  (let [cat-name (cat-model/name category-id)
        user-id  (:id user)
        category-id (Integer. category-id)]
    (main-layout user (str (:username user) "'s stuff") 
      [:div.container-fluid
       [:div.row-fluid
        [:div.span10
         [:div#add-new-bookmark [:h4 (user-link user "/bookmarks/new" "Add bookmark")]] 
         [:div#bookmarks  (bookmark-list user-id category-id)]]
        [:div.span2
         [:div#categories 
          [:h3 "Categories"]
          (category-list user-id category-id)
          (user-link user "/categories/new" "Add Category")
          (user-link user "/categories" "Manage Categories")]
         [:div#bookmarklets 
          [:h4.bookmarklet "Bookmarklet"]
          [:span.icon-question-sign]
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


(defn bookmark-list [user-id & [category-id]]
  [:div.bookmark-list
   (let [bookmarks (bm-model/bookmarks user-id category-id)
         bm-list (for [bm bookmarks]
                   (bm-views/display-bookmark bm))]
     (if (seq bm-list)
       bm-list
       [:p "No bookmarks yet for this category."]))])


;; TODO: change category with ajax / pushstate?
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


;; TODO: find out why do is a special form?
;(defmacro my-do [& exprs]
;(let [results (doall (for [expr exprs]
;(eval expr)))]
;(last results)))

;(my-do (println "test") (+ 3 4))
