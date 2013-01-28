(ns bookmarking.models.entities
  (:require [korma.core :refer :all]
            [korma.db :refer [defdb postgres]]
            [bookmarking.env :refer [db-map]]))

(defdb db (postgres db-map))

;; TODO: reconcile model names and entity names (entities are currently plural)

(declare bookmarks users)

;; Handling many-to-many:
;(select bookmarks
        ;(fields :users.name :urls.url)
        ;(join users {:bookmarks.user_id :users.id})
        ;(join urls  {:bookmarks.url_id :urls.id}))

(defentity users)
(defentity urls)
(defentity categories)

(defentity users-categories
  (table :users_categories)
  (has-one users {:fk :user_id})
  (has-one categories {:fk :category_id}))

(defentity bookmarks
  (has-one users {:fk :user_id})
  (has-one urls  {:fk :url_id}))
