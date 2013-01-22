(ns bookmarking.views.home
  (:require 
    [bookmarking.views.util :refer [error-list]]
    [bookmarking.views.layouts.main :refer [main-layout sign-up-form login-form]]
    [hiccup.form :refer [form-to text-field password-field 
                         submit-button label hidden-field]]
    [hiccup.element :refer [link-to]]))

;; TODO: (defview symbol [title] body) -> (defn symbol [req & [errors]]
;;                                          (main-layout title (logged-in? req)
;;                                            ~@body))

(defn home [user & [extra]]
  (main-layout user "Asocial bookmarking" 
    [:h1 (link-to "/" "Working Title")]
    (when user
      [:h3 (str (:username user) " is logged in!")])
    (error-list (:errors extra))
    (sign-up-form "home" extra)
    (login-form "home" extra)))

(defn login [user & [extra]]
  (main-layout user "login"
    [:h1 "Sign In"]
    (error-list (:errors extra))
    (login-form "login" extra)
    [:p "Don't have an account?  " (link-to "/register" "Sign up")]))

(defn register [user & [extra]]
  (main-layout user "register" 
    [:h1 "Register"]
    (error-list (:errors extra))
    (sign-up-form "register" extra)))
