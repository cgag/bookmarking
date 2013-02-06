;;;; TODO: change to just bookmarking.util
(ns bookmarking.views.util
  (:require [clojure.string :as s]
            [hiccup.element :refer [link-to]]
            [cemerick.friend :as friend]
            [korma.core :refer [select fields]]))

;; TODO: I think these may not need to be macros, look into korma's select*
(defmacro select-field [field entity & body]
  `(~field (first (select ~entity
                          (fields [~field])
                          ~@body))))

(defmacro select-one [entity & body]
  `(first (select ~entity
                  ~@body)))

(defn key->field-name [k]
  (-> k
    name
    s/capitalize
    (s/replace #"_" " ")
    (s/replace #"-" " ")))

(defn remove-blanks [m]
  (into {}
        (for [[k v] m
              :when (not (s/blank? v))]
          [k v])))

(defn error-list [errors]
  (when errors
    [:div.error-list
     [:h4 "Uh oh: "]
     [:ul
      (for [k (keys errors)
            error (k errors)]
        [:li (str (key->field-name k) " " error)])]]))

(defn user-link [user path text]
  (let [user (or (:id user) user)]
    (link-to (str "/users/" user path) text)))

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
  (when (seq coll)
    coll))
