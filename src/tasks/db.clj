(ns tasks.db
  (:require [korma.core :refer :all]
            [lobos [core    :refer [migrate rollback]]
                   [config]]
            [bookmarking.models.entities :as entities]
            [bookmarking.models.bookmark :as bookmark]
            [bookmarking.models.user     :as user]
            [bookmarking.models.url      :as url]
            [cemerick.friend [credentials :as creds]]))

(def usernames ["curtis" "stephen" "brian" "henry" "justin" "user"])
(def urls (for [url ["example.com" "foobar.com" "google.com" "yahoo.com"
                     "youtube.com" "reddit.com" "news.ycombinator.com"
                     "facebook.com"]]
            (str "http://" url)))

(defn add-users []
  (doseq [username usernames]
    (insert entities/users
            (values (merge {:username username
                            :password (creds/hash-bcrypt username)}
                           (when (zero? (rand-int 2))
                             {:email (str username "@gmail.com")}))))))

(defn add-admin []
  (insert entities/users
          (values {:username "admin"
                   :password (creds/hash-bcrypt "admin")
                   :role "admin"})))

(defn add-urls []
  (doseq [url urls]
    (insert entities/urls
            (values {:url url}))))

(defn add-bookmarks []
  (doseq [user-id (range 1 (inc (count usernames)))
          url-id (take (inc (rand-int (count urls)))
                       (shuffle (range 1 (inc (count urls)))))]
    (insert entities/bookmarks
            (values (merge
                      {:user_id user-id 
                       :url_id url-id
                       :title (:url (url/by-id url-id))}
                      (when (zero? (rand-int 2))
                        {:category "testcat"}))))))

(defn seed []
  (add-users)
  (add-admin)
  (add-urls)
  (add-bookmarks))

(defn clear []
  (delete entities/users)
  (delete entities/urls)
  (delete entities/bookmarks))

(defn rebuild []
  (rollback :all)
  (migrate))

(def task-map
  {"seed" seed
   "clear" clear
   "rebuild" rebuild})

(defn -main [& tasks]
  (doseq [task tasks]
    ((task-map task))))
