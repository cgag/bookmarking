(ns bookmarking.models.boilerpipe
  (:require [bookmarking.views.layouts.main :refer [main-layout]]
            [hiccup.core :refer :all]
            [hiccup.form :refer :all]
            [clojure.string :as s])
  (:import de.l3s.boilerpipe.extractors.ArticleExtractor
           de.l3s.boilerpipe.extractors.DefaultExtractor))

(declare to-html)

(def ^:private fake-agent "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)")

(def article-extractor (ArticleExtractor/getInstance))
(def default-extractor (DefaultExtractor/getInstance))

(defprotocol TextExtractor
  "Extract the text from a given source"
  (get-text-p [source extractor]
    "Get text from a given source using "))

(extend-protocol TextExtractor
  java.net.URL
  (get-text-p [source extractor]
    (let [cookie-store (clj-http.cookies/cookie-store)
          resp    (clj-http.client/get (str source) {:cookie-store cookie-store
                                                     :headers {"User-Agent" fake-agent}})
          body-str (:body resp)]
      (get-text-p body-str extractor)))
  Object
  (get-text-p [source extractor]
    (.getText extractor source)))

(defn get-text
  "text-source can be a java.net.URL, a String, a Reader, etc
   TODO: Checkout boilerpipe source and figure out what else"
  [source & [extractor]]
  (let [extractor (or extractor article-extractor)]
    (get-text-p source extractor)))

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