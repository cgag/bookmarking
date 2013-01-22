;; TODO: don't use :refer :all, figure what exactly is used
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

;(defn open-global-if-necessary
  ;([db-spec] (open-global-if-necessary :default-connection db-spec))
  ;([conn-name db-spec]
   ;(try
     ;(open-global conn-name db-spec)
     ;(catch java.lang.Exception e
       ;(if (re-find #"by that name already exists" (.getMessage e))
         ;(println "Connection already exists, doing nothing.")
         ;(throw e))))))
