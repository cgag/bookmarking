(ns bookmarking.env
  (:require [environ.core :as e]))

(def db-map
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :user     (e/env :bm-db-user)
   :password (e/env :bm-db-password)
   :subname  (e/env :bm-db-subname)})
