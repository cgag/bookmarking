(ns bookmarking.views.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [boilerpipe-clj.core :as bp]
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

(defn boilerpipe-view [user url]
  (main-layout user (str "Plain text view for " url)
    [:div.row
     [:div.span12
      [:div.plain-text
       (bp/wrap-paragraphs (bp/get-url-text url))]]]))
