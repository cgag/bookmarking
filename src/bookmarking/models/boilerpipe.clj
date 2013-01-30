(ns bookmarking.models.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [clojure.string :as s])
  (:import de.l3s.boilerpipe.extractors.ArticleExtractor))

(def ^:private test-url "http://www.flightglobal.com/news/articles/elon-musk-boeing-787-battery-fundamentally-unsafe-381627/")

(def article-extractor (ArticleExtractor/getInstance))

(defn get-text [url]
  (.getText article-extractor (java.net.URL. url)))

(declare boilerpipe-form)

(defn boilerpipe-view [user]
  (main-layout user "Test boilerpipe"
               (boilerpipe-form)))

(defn boilerpipe-form []
  (form-to [:post "/boilerpipe"]
           (label "url" "url: ")
           (text-field "url")))

(defn handle-post [url]
  (s/replace (get-text url) #"\n" "<br />"))