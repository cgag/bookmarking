(ns bookmarking.models.category
  (:refer-clojure :exclude [name find])
  (:require [clojure.string :as s]
            [bookmarking.models.entities :as entities]
            [bookmarking.views.util :refer [select-field select-one]]
            [korma.core :refer [select where fields join delete
                                insert update values order sql-only
                                set-fields dry-run sqlfn raw exec-raw]]
            [korma.db   :refer [transaction]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of
                                           format-of]]))

(declare validate-category)


(defn name [category-id]
  (select-field :category entities/categories
                (fields :category)
                (where {:id (Integer. category-id)})))

(defn by-id [id]
  (select-one entities/categories
              (where {:id id})))

(defn by-name [cat-name]
  (select-one entities/categories
              (where {:category cat-name})))

(defn find [user-id cat-id]
  (select-one entities/users-categories
              (fields :categories.category :category_id :user_id)
              (where {:user_id     (Integer. user-id)
                      :category_id (Integer. cat-id)})
              (join entities/categories {:categories.id :category_id})))

(defn first-category [user-id]
  (select-one entities/users-categories
              (fields :categories.id)
              (where {:user_id (Integer. user-id)})
              (join entities/categories {:category_id :categories.id})
              (order :categories.id)))

(defn- create-cat! [cat-name]
  (insert entities/categories
          (values {:category cat-name})))

(defn create! [user-id cat-name]
  (let [cat-params {:category cat-name
                    :user_id  user-id}
        errors (validate-category cat-params)]
    (if (seq errors)
      {:errors errors}
      (transaction
       (let [category (or (by-name cat-name)
                          (insert entities/categories
                                  (values {:category cat-name})))]
         (insert entities/users-categories
                 (values {:user_id (Integer. user-id)
                          :category_id (:id category)})))))))

(declare has-category?)

(defn update! [user-id cat-id params]
  (let [[user-id cat-id] [(Integer. user-id) (Integer. cat-id)]
        new-name (:new-name params)
        errors   (when (has-category? user-id new-name)
                   {:errors {:user #{(str "already has category " new-name)}}})]
    (if errors
      errors
      (let [new-cat-id  (:id (or (by-name new-name)
                                 (create-cat! new-name)))]
        (transaction
         (update entities/users-categories
                 (set-fields {:category_id new-cat-id})
                 (where {:user_id user-id
                         :category_id cat-id}))
         (update entities/bookmarks
                 (set-fields {:category_id new-cat-id})
                 (where {:user_id user-id
                         :category_id cat-id})))))))

(defn exists? [cat-name]
  (boolean (by-name cat-name)))

(defn id [cat-name]
  (select-field :id entities/categories
                (where {:category cat-name})))

(defn has-category? [user-id cat-name]
  (boolean
   (select-one entities/users-categories
               (where {:user_id (Integer. user-id)
                       :category_id (id cat-name)}))))

(defn has-category-id? [user-id cat-id]
  (boolean
   (select-one entities/users-categories
               (where {:user_id (Integer. user-id)
                       :category_id (Integer. cat-id)}))))

(defn categories [user-id]
  (select entities/users-categories
          (fields :category_id :categories.category :user_id)
          (where {:user_id (Integer. user-id)})
          (join entities/categories {:category_id :categories.id})
          (order :created_at)))


(defn delete! [user-id cat-id]
  (delete entities/users-categories
          (where {:user_id (Integer. user-id)
                  :category_id (Integer. cat-id)})))

;; Validations

(defn unique-category [m]
  (let [cat-name (:category m)
        user-id  (:user_id m)]
    (if (has-category? user-id cat-name) 
      [false {:user #{(str "already has category " cat-name)}}]
      [true  {}])))

(def validate-category
  (validation-set
   unique-category
   (presence-of :user_id)
   (presence-of :category)
   (format-of   :category :format #".*")))
