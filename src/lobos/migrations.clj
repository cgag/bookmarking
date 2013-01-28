(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:require [lobos [core :refer [create drop migrate rollback]]
                   [migration :refer [defmigration]]
                   [schema :refer [table varchar integer timestamp default unique]]
                   [helpers :refer [timestamps surrogate-key]]
                   [config]]))

;; TODO: truncate varchars that are too long?

(defmigration add-table-users
  (up [] (create
          (table :users
                 (surrogate-key)
                 (timestamps)
                 (varchar :username 30 :unique :not-null)
                 (varchar :password 100 :not-null)
                 (varchar :email 300)
                 (varchar :role 5 :not-null (default "user")))))
  (down [] (drop (table :users))))

(defmigration add-table-urls
  (up [] (create
          (table :urls
                 (surrogate-key)
                 (timestamps)
                 (varchar :url 500 :unique :not-null))))
  (down [] (drop (table :urls))))

(defmigration add-table-categories
  (up [] (create
          (table :categories
                 (timestamps)
                 (surrogate-key)
                 (varchar :category 30 :not-null (default "default")))))
  (down [] (drop (table :categories))))

(defmigration add-table-bookmarks
  (up [] (create
          (table :bookmarks
                 (timestamps)
                 (integer :user_id [:refer :users :id :on-delete :cascade])
                 (integer :url_id  [:refer :urls  :id :on-delete :cascade])
                 (integer :category_id [:refer :categories :id :on-delete :cascade])
                 (varchar :title 2000)
                 (unique  [:user_id :url_id :category_id :title]))))
  (down [] (drop (table :bookmarks))))

(defmigration add-table-users-categories
  (up [] (create
          (table :users_categories
                 (timestamps)
                 (integer :category_id [:refer :categories :id :on-delete :cascade])
                 (integer :user_id     [:refer :users :id :on-delete :cascade])
                 (unique [:user_id :category_id]))))
  (down [] (drop (table :users_categories))))

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
