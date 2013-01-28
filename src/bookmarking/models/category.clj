(ns bookmarking.models.category
  (:require [bookmarking.models.entities :as entities]
            [korma.core :refer [select where fields join]]))

(defn name [category-id]
  (:category
   (first
    (select entities/categories
                      (fields [:category])
                      (where {:id category-id})))))
