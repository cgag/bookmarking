(ns tasks.db
  (:require [korma.core :refer :all]
            [lobos [core    :refer [migrate rollback]]
                   [config]]
            [bookmarking.models.entities :as entities]
            [bookmarking.models.bookmark :as bookmark]
            [bookmarking.models.user     :as user]
            [bookmarking.models.url      :as url]
            [cemerick.friend [credentials :as creds]]))

(defmacro with-probability [p & body]
  `(when (< (rand-int 100) (* 100 ~p))
     ~@body))

(def usernames ["curtis" "stephen" "brian" "henry" "justin" "user"])
(def urls (for [url ["example.com" "foobar.com" "google.com" "yahoo.com"
                     "youtube.com" "reddit.com" "news.ycombinator.com"
                     "facebook.com"]]
            (str "http://" url)))

(def categories ["default" "category1" "category2" "category3"])

(defn add-users []
  (doseq [username usernames]
    (insert entities/users
            (values (merge {:username username
                            :password (creds/hash-bcrypt username)}
                           (with-probability 1/2
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
                       (shuffle (range 1 (inc (count urls)))))
          cat-id [(inc (rand-int (count categories)))]]
    (insert entities/bookmarks
            (values (merge
                     {:user_id user-id 
                      :url_id url-id
                      :title (:url (url/by-id url-id))
                      :category_id cat-id})))))

(defn add-categories []
  (doseq [category categories]
    (insert entities/categories
            (values {:category category}))))

(defn add-users-categories []
  (doseq [user-id (range 1 (inc (count usernames)))
          cat-id (range 1 (inc (count categories)))]
    (insert entities/users-categories
            (values {:user_id user-id
                     :category_id cat-id}))))

(defn tons-of-bookmarks []
  (dotimes [x 300]
    (let [url-id (:id (insert entities/urls
                              (values {:url (str "http://url" x)})))]
      (insert entities/bookmarks
              (values {:user_id 1
                       :url_id url-id
                       :category_id 1
                       :title  (str "http://url" x)})))))

(defn seed []
  (add-users)
  (add-admin)
  (add-categories)
  (add-users-categories)
  (add-urls)
  (add-bookmarks)
  (tons-of-bookmarks))

(defn clear []
  )

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
