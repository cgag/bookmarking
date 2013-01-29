(ns bookmarking.models.category
  (:require [bookmarking.models.entities :as entities]
            [korma.core :refer [select where fields join
                                insert update values]]
            [korma.db   :refer [transaction]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of
                                           format-of]]))

(declare validate-category)

(defn name [category-id]
  (:category
   (first
    (select entities/categories
                      (fields [:category])
                      (where {:id category-id})))))

(defn by-name [cat-name]
  (first
   (select entities/categories
           (where {:category cat-name}))))

(defn create! [user-id cat-name]
  (let [cat-params {:category cat-name}
        errors (validate-category cat-params)]
    (if (seq errors)
      {:errors errors}
      (transaction
       (let [category (insert entities/categories
                              (values cat-params))]
         (insert entities/users-categories
                 (values {:user_id (Integer. user-id)
                          :category_id (:id category)})))))))

;; Validations

(defn unique-category [m]
  (if (by-name (:category m))
    [false {:category #{"already exists"}}]
    [true  {}]))

(def validate-category
  (validation-set
   unique-category
   (presence-of :category)
   (format-of   :category :format #".*")))
