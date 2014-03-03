(ns lobos.helpers
  (:refer-clojure :exclude [drop])
  (:require [lobos [schema :refer [integer timestamp default table]]
                   [core :refer [drop]]
                   [connectivity :refer [open-global]]]))

(defn timestamps [table]
  (-> table
    (timestamp :updated_at)
    (timestamp :created_at (default (now)))))

(defn surrogate-key [table]
  (integer table :id :auto-inc :primary-key))
