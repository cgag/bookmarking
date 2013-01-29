(ns bookmarking.views.categories
  (:require [bookmarking.models.category :as cat]
            [bookmarking.views.layouts.main :refer [main-layout]]
            [bookmarking.views.util :refer [error-list]]
            [hiccup.form :refer [form-to text-field submit-button
                                 label]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of
                                           format-of]]))

(defn new [user & [category]]
  (main-layout user "Create New Category"
               (new-category-form (:id user) category)))

(defn new-category-form [user-id & [{:keys [errors]}]]
  [:div#new-category-wrapper
   (when errors (error-list errors))
   [:fieldset
    [:ul#new-category-form
     (form-to [:post (str "/users/" user-id "/categories")]
              [:li (label "category" "Category: ")
               (text-field "category" "Category")]
              [:li (label "submit" "")
               (submit-button "Submit")])]]])

