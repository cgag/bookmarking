(ns bookmarking.models.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [clojure.string :as s])
  (:import de.l3s.boilerpipe.extractors.ArticleExtractor
           de.l3s.boilerpipe.extractors.DefaultExtractor))

(declare to-html)

(def ^:private user-agent "Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0")

(def article-extractor (ArticleExtractor/getInstance))
(def default-extractor (DefaultExtractor/getInstance))

(defprotocol TextExtractor
  "Extract the text from a given source"
  (extract-text [source extractor]
    "Get text from a given source using "))

(extend-protocol TextExtractor
  java.net.URL
  (extract-text [source extractor]
    (let [cookie-store (clj-http.cookies/cookie-store)
          resp    (clj-http.client/get (str source) {:cookie-store cookie-store
                                                     :headers {"User-Agent" user-agent}})
          body-str (:body resp)]
      (extract-text body-str extractor)))
  Object
  (extract-text [source extractor]
    (.getText extractor source)))

(defn get-text
  "text-source can be a java.net.URL, a String, a Reader, etc
   TODO: Checkout boilerpipe source and figure out what else"
  [source & [extractor]]
  (let [extractor (or extractor article-extractor)]
    (extract-text source extractor)))

(defn get-html [source & [extractor]]
  (let [text (get-text source extractor)]
    (to-html text)))

(defn to-html [text]
  (let [paragraphs (s/split text #"\n")]
    (->> paragraphs
         (map #(str "<p>" % "</p>"))
         (s/join ""))))

(declare boilerpipe-form)

(defn boilerpipe-view [user]
  (main-layout user "Test boilerpipe"
               (boilerpipe-form)))

(defn boilerpipe-form []
  (form-to [:post "/boilerpipe"]
           (label "url" "url: ")
           (text-field "url")
           (submit-button "Get Article")))

(defn get-url-text [url]
  (get-html (java.net.URL. url)))