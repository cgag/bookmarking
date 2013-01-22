(ns lobos.config
  (:require [lobos.connectivity :as conn]
            [bookmarking.env :as env]))

(conn/open-global env/db-map)
