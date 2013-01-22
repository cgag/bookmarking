(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:require [lobos [core :refer [create drop migrate rollback]]
                   [migration :refer [defmigration]]
                   [schema :refer [table varchar integer timestamp default unique]]
                   [helpers :refer [timestamps surrogate-key]]
                   [config]]))

(defmigration add-urls-table
  (up [] (create
           (table :urls
                  (surrogate-key)
                  (timestamps)
                  (varchar :url 500 :unique :not-null))))
  (down [] (drop (table :urls))))

(defmigration add-users-table
  (up [] (create
           (table :users
                  (surrogate-key)
                  (timestamps)
                  (varchar :username 30 :unique :not-null)
                  (varchar :password 100 :not-null)
                  (varchar :email 100)
                  (varchar :role 5 :not-null (default "user")))))
  (down [] (drop (table :users))))

(defmigration add-bookmarks-table
  (up [] (create
           (table :bookmarks
                  (timestamps)
                  (integer :user_id [:refer :users :id :on-delete :cascade])
                  (integer :url_id  [:refer :urls  :id :on-delete :cascade])
                  (varchar :category :not-null (default "default"))
                  (varchar :title 500)
                  (unique  [:user_id :url_id :category :title]))))
  (down [] (drop (table :bookmarks))))


;; probably a better way to handle this in general, but how is this for a macro?
(defmacro ignore-exceptions [& exprs]
  `(do
     ~@(for [expr exprs]
         `(try ~expr (catch Exception e# (println e#))))))

(defn drop-all-tables []
  (ignore-exceptions
    (drop (table :bookmarks))
    (drop (table :users))
    (drop (table :lobos_migrations))))
