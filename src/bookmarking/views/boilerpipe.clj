(ns bookmarking.views.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [hiccup.form :refer :all]))

(declare boilerpipe-form)

(defn boilerpipe-form-view [user]
  (main-layout user "Test boilerpipe"
    (boilerpipe-form)))

(defn boilerpipe-form []
  (form-to [:post "/boilerpipe"]
           (label "url" "url: ")
           (text-field "url")
           (submit-button "Get Article")))
