(ns bookmarking.models.bookmark
  (:require [korma.core :refer [select modifier where insert
                                aggregate values fields join
                                delete dry-run order update
                                set-fields offset limit exec-raw
                                sql-only]]
            [clojure.string :as s]
            [clojure.set :as set]
            [net.cgrand.enlive-html :as enlive]
            [clj-http.client :as http]
            [bookmarking.models.entities :as entities]
            [bookmarking.models.url :as url]
            [bookmarking.models.category :as cat]
            [bookmarking.views.util :refer [remove-blanks
                                            select-field
                                            select-one]]
            [validateur.validation :refer [validation-set presence-of
                                           numericality-of length-of]]))

;;TODO: should this model only be for validations?  What should getting a 
;; user's bookmarks look like?
;; (user/bookmarks user-id) or (bookmark/bookmarks user-id)
;; How should has-one/has-many relationships be modeled?

(declare validate-bookmark validate-update find-bookmark
         num-bookmarks)

(defn bookmark-params [params]
  (let [bm-params (-> params
                      (select-keys [:user-id :category-id :title])
                      (update-in [:category-id] #(or % 1))
                      (set/rename-keys {:user-id :user_id
                                        :category-id :category_id})
                      (update-in [:user_id] #(Integer. %))
                      (update-in [:category_id] #(Integer. %)))]
    bm-params))


(defn save-title [bm url & [{:keys [block-for]}]]
  (let [ftr (future (let [resp  (:body (http/get url {:conn-timeout 5000
                                                      :socket-timeout 15000}))
                          html  (enlive/html-resource (java.io.StringReader. resp))
                          title (-> (enlive/select html [:title])
                                  first
                                  :content
                                  first)]
                      (update entities/bookmarks
                              (set-fields {:title title})
                              (where (select-keys bm [:user_id :url_id])))))]
    (when block-for
      (deref ftr block-for "Too long"))))


;; TODO: create a kibit rule for (if-not (empty? x)) -> (if (seq x))
(defn create! [params]
  (let [url-params (select-keys params [:url])
        url (or (url/by-url (:url url-params)) 
                (url/create! url-params))] 
    (if (seq (:errors url))
      {:errors (:errors url)}
      (let [bm-params (bookmark-params params)
            bookmark  (assoc bm-params :url_id (:id url)) 
            errors    (validate-bookmark bookmark)] 
        (if (seq errors)
          {:errors errors}
          (let [new-bm (insert entities/bookmarks
                               (values bookmark))]
            (when (s/blank? (:title bookmark))
                (save-title new-bm (:url url)))
            {:bookmark new-bm}))))))


;; TODO: find a cleaner way to do this
(defn update-params [params]
  (let [uparams (-> params 
                    (select-keys [:url :title :category])
                    remove-blanks)
        new-url-id      (when (:url uparams)
                          (:id (or (url/by-url (:url uparams))
                                   (url/create! {:url (:url uparams)}))))
        new-cat-id (when (:category uparams)
                     (or (:id (cat/by-name (:category uparams)))
                         (:category_id (cat/create! (:user-id params) (:category params)))))
        uparams (dissoc uparams :category)
        uparams (if-not new-url-id
                  uparams
                  (-> uparams
                      (dissoc :url)
                      (assoc :url_id new-url-id)))
        uparams (if-not new-cat-id
                  uparams
                  (-> uparams
                      (dissoc :category)
                      (assoc :category_id new-cat-id)))]
    uparams))


(defn update! [user-id url-id params]
  (let [current-cat (:current-cat params)
        current-bm (find-bookmark user-id url-id current-cat)
        uparams (update-params params)]
    (update entities/bookmarks
            (where {:user_id (Integer. user-id)
                    :url_id  (Integer. url-id)
                    :category_id (Integer. current-cat)})
            (set-fields (merge current-bm
                               uparams)))))


(defn bookmarks [user-id cat-id {:keys [page per-page]}]
  (let [page (Integer. page)
        per-page (Integer. per-page)]
    [(select entities/bookmarks
             (where {:user_id (Integer. user-id)
                     :category_id (Integer. cat-id)})
             (limit per-page)
             (offset (* (dec page) per-page))
             (order :created_at :DESC))
     (num-bookmarks user-id cat-id)]))


(defn num-bookmarks [user-id cat-id]
  (:count (first 
           (select entities/bookmarks
                   (aggregate (count :*) :count) 
                   (where {:users.id (Integer. user-id)
                           :category_id (Integer. cat-id)})
                   (join entities/users {:bookmarks.user_id :users.id})
                   (join entities/urls  {:bookmarks.url_id  :urls.id})))))


(defn find-bookmark [user-id url-id category_id]
  (select-one entities/bookmarks
              (where {:url_id   (Integer. url-id)
                      :user_id  (Integer. user-id)
                      :category_id (Integer. category_id)})))


(defn delete! [user-id url-id cat-id]
  (delete entities/bookmarks
          (where {:user_id (Integer. user-id)
                  :url_id  (Integer. url-id)
                  :category_id (Integer. cat-id)})))

(declare like make-query words)
(defn search
  "Search across all categories"
  [user-id cat-id query & [{:keys [page per-page] :or {page 1 per-page 50}}]]
  (let [tsquery (make-query "&" query) 
        user-id (Integer. user-id)
        cat-id  (Integer. cat-id)
        page    (Integer. page)
        per-page (Integer. per-page)
        offset  (* (dec page) per-page)
        limit   per-page
        base-query (str
                    "FROM
                        bookmarks b,
                        urls u
                    WHERE
                        b.user_id = ?   AND
                      --  b.category_id = ? AND
                        b.url_id = u.id AND
                        ( to_tsvector(b.title) @@ to_tsquery(?)
                        OR "
                    (like "u.url" query)
                    ")")
        real-query (str "SELECT * " base-query "OFFSET ? LIMIT ?")
        count-query (str "SELECT COUNT(*) " base-query)]
    [(exec-raw [real-query [user-id #_cat-id tsquery offset limit]] :results)
     (:count (first (exec-raw [count-query [user-id #_cat-id tsquery]] :results)))]))

(defn words [s] (s/split s #"\s+"))

(defn make-query 
  "hello world => hello:* <& or |> world:*"
  [op query]
  (s/join (str " " op " ")
          (for [word (words query)]
            (str word ":*"))))

(defn like
  "x LIKE '%query_word_1%'
   OR ...
   OR x LIKE '%query_word_n%'"
  [x query]
  (->> (for [word (words query)]
         (str x " LIKE '%" word "%'"))
       (interpose " OR ")
       (apply str)))


;; validations

(defn unique-bookmark [bm]
  (let [{:keys [user_id url_id category_id]} bm
        from-db (find-bookmark user_id url_id category_id)]
    (if from-db
      [false {:bookmark #{"already exists."}}]
      [true {}])))


(def validate-bookmark (validation-set
                        (presence-of :url_id)
                        (presence-of :user_id)
                        (presence-of :category_id)
                        (numericality-of :url_id)
                        (numericality-of :user_id)
                        (numericality-of :category_id)
                        unique-bookmark))

(comment (def time-query "SELECT bookmarks.* FROM bookmarks WHERE (bookmarks.user_id = 1 AND bookmarks.category_id = 1) ORDER BY bookmarks.created_at DESC LIMIT 50 OFFSET 0;"))
