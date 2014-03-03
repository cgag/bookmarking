(ns lobos.config
  (:require [lobos.connectivity :as conn]
            [bookmarking.env :as env]))

(defn open-global-db! []
  (conn/open-global env/db-map))
