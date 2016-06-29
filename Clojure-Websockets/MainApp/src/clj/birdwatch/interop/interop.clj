(ns birdwatch.interop.interop
  (:require [clojure.core.match :refer [match]]
            [taoensso.carmine :as car]
            [clojure.tools.logging :as log]))

(defn- msg-handler-fn
  "create handler function for messages from Redis Pub/Sub"
  [put-fn]
  (fn [[msg-type _topic msg]]
    (when (= msg-type "message")
      (put-fn msg))))

(defn subscribe-topic
  "subscribe to topic, put items on specified channel"
  [put-fn conn topic]
  (car/with-new-pubsub-listener
    (:spec conn)
    {"matches" (msg-handler-fn put-fn)}
    (car/subscribe topic)))

(defn iop-state-fn
  "Returns function for making state of the interop-component while using provided configuration."
  [conf]
  (fn [put-fn]
    (let [redis-host (:redis-host conf)
          redis-port (:redis-port conf)
          conn {:pool {}
                :spec {:host redis-host
                       :port redis-port}}
          listener (subscribe-topic put-fn conn "matches")]
      (log/info "Redis connection started to" redis-host redis-port)
      {:state (atom {:conf     conf
                     :conn     conn
                     :listener listener})})))

(defn cmp-map
  "Create component for communicating with Redis."
  [cmp-id conf]
  {:cmp-id      cmp-id
   :state-fn    (iop-state-fn conf)
   :handler-map {}})
