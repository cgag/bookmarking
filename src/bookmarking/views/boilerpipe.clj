(ns bookmarking.views.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [boilerpipe-clj.core :as bp]
            [hiccup.form :refer :all]
            [clj-http  [client :as http]
                       [cookies :as cookies]]))

(def user-agent "Mozilla/5.0 (Windows NT 6.1; rv:10.0 Gecko/20100101 Firefox/)10.0")

(declare boilerpipe-form)

(defn boilerpipe-form-view [user]
  (main-layout user "Test boilerpipe"
    (boilerpipe-form)))

(defn boilerpipe-form []
  (form-to [:get "/plain-text"]
           (label "url" "url: ")
           (text-field "url")
           (submit-button "Get Article")))

(defn boilerpipe-view [user url]
  (main-layout user (str "Plain text view for " url)
    [:div.row
     [:div.span12
      [:div.plain-text
       (let [resp (http/get url {:cookie-stor (cookies/cookie-store)
                                 :headers {"User-Agent" user-agent}}) 
             text (:body resp)]
         (bp/get-text-as-html text))]]]))
