(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:require [lobos [core :refer [create drop migrate rollback]]
                   [migration :refer [defmigration]]
                   [schema :refer [table varchar index integer timestamp default unique]]
                   [helpers :refer [timestamps surrogate-key]]
                   [config :refer [open-global-db!]]]))

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
                 (varchar :category 30 :not-null (default "default"))
                 (unique [:category]))))
  (down [] (drop (table :categories))))


(defmigration add-table-bookmarks
  (up [] (create
          (table :bookmarks
                 (timestamps)
                 (integer :user_id     :not-null [:refer :users :id :on-delete :cascade])
                 (integer :url_id      :not-null [:refer :urls  :id :on-delete :cascade])
                 (integer :category_id :not-null [:refer :categories :id :on-delete :cascade])
                 (varchar :title 2000)
                 (unique  [:user_id :url_id :category_id :title]))))
  (down [] (drop (table :bookmarks))))


(defmigration add-table-users-categories
  (up [] (create
          (table :users_categories
                 (timestamps)
                 (integer :category_id :not-null [:refer :categories :id :on-delete :cascade])
                 (integer :user_id     :not-null [:refer :users :id :on-delete :cascade])
                 (unique [:user_id :category_id]))))
  (down [] (drop (table :users_categories))))

(defmigration add-bookmarks-index
  (up [] (create (index :bookmarks [:user_id :category_id])))
  (down [] (drop (index :bookmarks [:user_id :category_id]))))

(defn run-migrations []
  (binding [lobos.migration/*reload-migrations* false] 
    (lobos.core/migrate)))

