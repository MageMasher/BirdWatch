(ns birdwatch.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [<! timeout put!]]))

(defn by-id [id] (.getElementById js/document id))
(defn elem-width [elem] (aget elem "offsetWidth"))

(defn msg-loop
  "Run a go-loop that puts message m on channel c every t1 + t2 seconds , where the send
   of msg occurs between t1 and t2. Then publishes the result of f on channel c.
   Also exists in another arity that only takes t1 as the pre-exection
   wait time, with post-execution set to zero."
  ([c m t1 t2]
   (go-loop []
            (<! (timeout (* t1 1000)))
            (put! c m)
            (<! (timeout (* t2 1000)))
            (recur)))
  ([c m t1] (msg-loop c m t1 0)))

(defn search-hash []
  (subs (js/decodeURIComponent (aget js/window "location" "hash")) 1))

(defn tweets-by-order
  "Find top n tweets by specified order."
  [tweets-map order]
  (fn [app n skip]
    (vec (map (fn [m] ((keyword (first m)) (tweets-map app)))
              (take n (drop (* n skip) (order app)))))))

(defn query-string
  "format and modify query string"
  [state]
  {:query_string {:default_field "text"
                  :default_operator "AND"
                  :query (str "(" (:search state) ") AND lang:en")}})
