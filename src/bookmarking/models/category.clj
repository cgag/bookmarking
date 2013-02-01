(ns bookmarking.models.category
  (:refer-clojure :exclude [name])
  (:require [bookmarking.models.entities :as entities]
            [bookmarking.views.util :refer [select-field select-one]]
            [korma.core :refer [select where fields join
                                insert update values]]
            [korma.db   :refer [transaction]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of
                                           format-of]]))

(declare validate-category)

(defn name [category-id]
  (select-field :category entities/categories
                (fields [:category])
                (where {:id category-id})))

(defn by-name [cat-name]
  (select-one entities/categories
          (where {:category cat-name})))

(defn by-id [id]
  (select-one entities/categories
              (where {:id id})))

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
