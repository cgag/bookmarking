(ns bookmarking.views.users
  (:require [hiccup.core :refer :all]
            [hiccup.element :refer [link-to]]
            [hiccup.form :refer :all]
            [environ.core :as e]
            [bookmarking.views.util :refer [user-link]]
            [bookmarking.models.user :as user]
            [bookmarking.models.bookmark :as bm-model]
            [bookmarking.views.bookmarks :as bm-views]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [cemerick.friend :as friend]))

;; TODO: why are the views plural and the models singular?

(declare bookmark-list category-list bookmarklet)

(defn new [req]
  (html [:p "Sign up page"]))

;; TODO: This either needs access to the request (they all would),
;; or logged-in? needs to be passed in, probably as a part of an options map,
;; probably the latter, but currently teh former
;; TODO: make functions like bookmark-list take either the id or the user map itself
;; TODO: create a function for makingl inks of the form (str "/users/" (:id user))
(defn show [user & [{:keys [category]}]]
  (let [category (or category "default")] 
    (main-layout user (str (:username user) "'s stuff") 
      [:div.container-fluid
       [:div.row-fluid
        [:div.span10
         [:div#add-new-bookmark (user-link user "/bookmarks/new" "Add bookmark")] 
         [:div#bookmarks  (bookmark-list (:id user) category)]]
        [:div.span2
         [:h3 "Categories"]
         [:div#categories (category-list (:id user) category)]
         [:div.bookmarklet [:a {:href (bookmarklet (:id user) category)}
                            (str category " Bookmarklet")]]]]])))

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

(defn bookmark-list [user-id & [category]]
  [:div.bookmark-list
   (let [bookmarks (bm-model/bookmarks user-id {:category category})]
     (for [bm bookmarks]
       (bm-views/display-bookmark bm)))])

;; TODO: categories should be links, categories other than default
;; should be passed as get params in the url
;; TODO: change category with ajax / pushstate
(defn category-list [user-id & [current-cat]]
  (let [current-cat (or current-cat "default")]
    (for [category (bm-model/categories user-id)]
      [:li (when (= category current-cat)
             {:class "current-category"}) 
       (link-to (str "?category=" category) category)])))

;; TODO: category links dont' work when you're viwing the profile on "/" due to trying to view the homepage while logged in
;; TODO: Make sure the function to detect if bookmark already exists says no if the bookmark already exists but is in a different category
(defn bookmarklet [user-id category]
  (let [host (e/env :bm-host)
        port (e/env :bm-port)
        ssl-port (e/env :bm-ssl-port)]
    (str "javascript:(function(){
         var newScript = document.createElement('scr' + 'ipt');
         var url = encodeURIComponent(document.location.href);
         var title = encodeURIComponent(document.title);
         var category = encodeURIComponent('" category "');"
         "var userid = encodeURIComponent('" user-id "');"
         "var scheme = document.location.protocol;"
         "var port = (scheme === 'http:') ? '" port "' : '" ssl-port "';"
         "newScript.type='text/javascript';"
         "newScript.src= scheme +  '//" host ":' + port + '/js/bookmarklet.js?dummy=' + Math.random() + '&url=' + url + '&title=' + title + '&category=' + category + '&userid=' + userid;"
         "document.body.appendChild(newScript);}())")))

;//        var scheme = document.location.protocol;
;//        var port = (scheme === 'http:') ? 3000 : 8443 
;//          newScript.src = scheme + '//localhost:' + port + '/js/bookmarklet.js?dummy=' + Math.random()
;//          + '&url=' + url + '&title=' + title + '&category=' + category + '&userid=' + userid;\n 

;; TODO: find out why do is a special form?
;(defmacro my-do [& exprs]
;(let [results (doall (for [expr exprs]
;(eval expr)))]
;(last results)))

;(my-do (println "test") (+ 3 4))
