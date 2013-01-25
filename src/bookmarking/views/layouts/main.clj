(ns bookmarking.views.layouts.main
  (:require [hiccup  [core :refer :all]
                     [element :refer [link-to]]
                     [page :refer [html5 include-css include-js]]
                     [form :refer :all]]
            [bookmarking.views.util :refer [empty->nil]]))

(defn not-blank [s]
  (when-not (clojure.string/blank? s)
    s))

(defn placeholder [v default]
  (or (not-blank v) default))

(defn error-class [errors & fields]
  (when (some (set fields) (keys errors))
    "error"))

;; TODO: need a way to repopulate fields with previous entries
;; TODO: Change this to register-form
;; TODO: Currently highlighting errors for both fields at once,
;;       error fields need custom names or only have one form on the page (probably the latter)
(defn sign-up-form 
  "page is used as a hidden value to identify which page the form was submitted from
  (either the home page or the registration page at the moment)"
  [page & [{:keys [errors params]}]]
  [:div#sign-up-wrapper
   [:fieldset 
    [:ul#sign-up-form
     (form-to [:post "/users"]
              [:li (text-field {:class       (error-class errors :username)
                                :placeholder (placeholder (:username params) "username")} 
                               "username")]
              [:li (text-field {:class       (error-class errors :email)
                                :placeholder (placeholder (:email params) "email address")}
                               "email")]
              [:li (password-field {:class   (error-class errors :password :password_confirmation)
                                    :placeholder "password"} "password")]
              [:li (password-field {:class   (error-class errors :password :password_confirmation)
                                    :placeholder "verify password"} "password_confirmation")]
              [:li.hidden (hidden-field "form" page)]
              [:li (submit-button {:class "btn btn-large btn-primary"} "Create Account")])]]])

;; TODO: login should post to somewhere else?
;; TODO: make sure ref req gets filled on failed login
(defn login-form
  "Takes a string representing the page the form is on, as well as any
  errors, params from a failed request such as the username, and the
  referring request"
  [page & [{:keys [errors params ref-req] :as param-map}]]
  [:div#login-form-wrapper
   [:fieldset
    [:ul#login-form
     (form-to [:post "/"]
              [:li (text-field {:class (error-class errors :username)
                                :placeholder (placeholder (:username params) "username")}
                               "username" (:username params))]
              [:li (password-field {:class (error-class errors :password)
                                    :placeholder "password"}
                                   "password")]
              [:li.hidden (hidden-field "form" page)]
              [:li.hidden (hidden-field "ref-req" (pr-str ref-req))]
              [:li.hidden (hidden-field "get-params" (pr-str (empty->nil (select-keys params [:url :title :category :userid]))))]
              [:li (submit-button {:class "btn btn-large btn-primary"} 
                                  "Sign In")])]]])

;; TODO: apple icons?

;; TODO: move nav into header?
(def header [:header "Header"])

(defn dropdown-menu [text target & body]
  [:li {:class "dropdown"}
   (link-to {:role "button"
             :class "dropdown-toggle"
             :data-toggle "dropdown"}
            target
            [:span text [:b {:class "caret"}]])
   [:ul {:class "dropdown-menu" :role "menu"}
    body]])

(declare logged-in-menu logged-out-menu)

;; TODO: how to do this without polluting the html?
;; TODO: just do a dropdown menu for login / sign-up,
;; but fall back to dedicated page if no javascript
(defn nav
  [& [user]]
  [:div {:class "navbar navbar-inverse navbar-fixed-top"}
   [:div {:class "navbar-inner"}
    [:div {:class "container"}
     [:a {:class "btn btn-navbar" :data-toggle "collapse" :data-target ".nav-collapse"}
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]]
     [:a {:class "brand" :href "/"} "Bookmarking"]
     [:div {:class "nav-collapse collapse"}
      [:ul {:class "nav" :role "navigation"}]
      (let [nav-section [:ul {:class "nav pull-right" :role "navigation"}]]
        (if user
          (logged-in-menu user) 
          logged-out-menu))]]]])

(defn logged-in-menu [user]
  (let [nav-section [:ul {:class "nav pull-right" :role "navigation"}]]
    (-> nav-section
      (conj [:li 
             (link-to (str "/users/" (:id user) "/edit") (str (:username user)))])
      (conj [:li (link-to "/logout" "logout")]))))

(def logged-out-menu
  (let [nav-section [:ul {:class "nav pull-right" :role "navigation"}]]
    (-> nav-section
      (conj (dropdown-menu "Register" "/register"
                           (sign-up-form "home")))
      (conj (dropdown-menu "Sign In" "/" 
                           (login-form "home"))))))

(def footer
  [:footer "Footer goes here"])

;; TODO: actually put shiv in resources or use CDN
(def html5-shiv
  "<!--[if lt IE 9]>
  <script src=\"js/html5shiv.js\" media=\"all\"></script>
  <script src=\"js/html5shiv-printshiv.js\" media=\"print\"></script>
  <![endif]-->")

(defn google-analytics [ga-code]
  (format "<script>
          var _gaq=[['_setAccount','%s'],['_trackPageview']];
          (function(d,t){var g=d.createElement(t),s=d.getElementsByTagName(t)[0];
          g.src=('https:'==location.protocol?'//ssl':'//www')+'.google-analytics.com/ga.js';
          s.parentNode.insertBefore(g,s)}(document,'script'));
          </script>"
          ga-code))

;; TODO: probably not necessary
(def chrome-frame
  "<!--[if lt IE 7]>
  <p class=\"chromeframe\">You are using an <strong>outdated</strong> browser. Please <a href=\"http://browsehappy.com/\">upgrade your browser</a> or <a href=\"http://www.google.com/chromeframe/?redirect=true\">activate Google Chrome Frame</a> to improve your experience.</p>
  <![endif]-->")

(defn main-layout [user title & content]
  (html5 
    {:lang "en"}
    [:head
     [:meta {:charset "utf-8"}]
     [:title title]
     [:meta {:name "description" 
             :content "Simple asocial bookmarking service."}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1.0"}]
     (include-css "/css/mycss.css")
     (include-css "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/css/bootstrap-combined.min.css")]
    [:body
     chrome-frame
     (nav user)
     content
     footer
     (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/1.8.3/jquery.min.js")
     (include-js "//netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js")
     (include-js "/js/myjs.js")]))
