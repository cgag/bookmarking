(ns bookmarking.views.util
  (:require [clojure.string :as s]
            [hiccup.element :refer [link-to]]
            [cemerick.friend :as friend]))

(defn key->field-name [k]
  (-> k
    name
    s/capitalize
    (s/replace #"_" " ")))

(defn error-list [errors]
  (when errors
    [:div.error-list
     [:h4 "Uh oh: "]
     [:ul
      (for [k (keys errors)
            error (k errors)]
        [:li (str (key->field-name k) " " error)])]]))

(defn user-link [user path text]
  (link-to (str "/users/" (:id user) path) text))

(defn de-entify [text]
  (-> text
    (.replace "&quot;" "\"")
    (.replace "&amp;" "&")
    (.replace "&lt;" "<")
    (.replace "&gt;" ">")))

(defn query-str [m]
  (->> (for [[k v] m]
         (str (name k) "=" v))
    (interpose "&")
    (apply str)))

(defn empty->nil [coll]
  (when-not (empty? coll)
    coll))
