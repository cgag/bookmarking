(ns bookmarking.views.users
  (:require [hiccup.core :refer :all]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer :all]
            [environ.core :as e]
            [bookmarking.views.util :refer [user-link]]
            [bookmarking.models.user :as user]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.models.category :as category]
            [bookmarking.views.bookmarks :as bm-views]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [cemerick.friend :as friend]))

;; TODO: why are the views plural and the models singular?

(declare bookmark-list category-list bookmarklet)

;; TODO: This either needs access to the request (they all would),
;; or logged-in? needs to be passed in, probably as a part of an options map,
;; probably the latter, but currently teh former
;; TODO: make functions like bookmark-list take either the id or the user map itself
;; TODO: create a function for makingl inks of the form (str "/users/" (:id user))
(defn show [user & [{:keys [category]}]]
  (let [category-id (Integer. (or category 1))
        cat-name (category/name category-id)
        user-id (:id user)]
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
          (user-link user "/categories/new" "Add Category")]
         [:div#bookmarklets 
          [:h4.bookmarklet "Bookmarklet"]
          [:span.icon-question-sign]
          [:div.bookmarklet
           [:span.label [:a.bookmarklet {:href (bookmarklet user-id category-id)} cat-name]]]]]]])))

;(bookmarklet-list (:id user))

;; TODO: categories should not be part of bm-model
;; TODO: should categories be a parm? calling it in cat-list too
(defn bookmarklet-list [user-id]
  (for [category (bm-model/categories user-id)
        :let [cat-id (:category_id category)
              cat-name (:category category)]]
    [:div.bookmarklet [:span.label [:a.bookmarklet {:href (bookmarklet user-id cat-id)} cat-name]]]))

;; TODO: actually handle the post request
(defn edit [user]
  (main-layout user "Change your password"
    [:div#edit-user-wrapper
     [:fieldset
      [:legend "Password"]
      [:ul#edit-user-form
       (form-to [:post (str "/users/" (:id user))]
                [:li (text-field {:placeholder "Current Password"} "current_pass")]
                [:li (text-field {:placeholder "New Passwword"} "new_pass")]
                [:li (text-field {:placeholder "Confirm New Password"} "new_pass_conf")]
                [:li (submit-button {:class "btn btn-large btn-primary"} "Save changes")])]]]))

(defn update! [req]
  (html [:p "idk how to handle POST requests"]))

(defn bookmark-list [user-id & [category-id]]
  [:div.bookmark-list
   (let [bookmarks (bm-model/bookmarks user-id {:category-id category-id})]
     (for [bm bookmarks]
       (bm-views/display-bookmark bm)))])

;; TODO: categories should be links, categories other than default
;; should be passed as get params in the url
;; TODO: change category with ajax / pushstate
(defn category-list [user-id & [current-cat]]
  (for [category (bm-model/categories user-id)
        :let [cat-name (:category category)
              cat-id   (:category_id category)]]
    [:li (when (= cat-id current-cat)
           {:class "current-category"}) 
     (link-to (str "?category=" cat-id) cat-name)]))

;; TODO: category links dont' work when you're viwing the profile on "/" due to trying to view the homepage while logged in
;; TODO: Make sure the function to detect if bookmark already exists says no if the bookmark already exists but is in a different category
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
