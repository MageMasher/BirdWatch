(ns birdwatch.percolator.percolator
  (:require [clojure.core.match :refer [match]]
            [pandect.core :as p]
            [matthiasn.systems-toolbox.handler-utils :as hu]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.percolation :as perc]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]))

(defn percolator-state-fn
  "Returns function for making state while using provided configuration."
  [conf]
  (fn [_put-fn]
    (let [es-address (:es-address conf)
          conn (esr/connect es-address)]
      (log/info "Percolator component started with ES connection to" es-address)
      {:state (atom {:conf           conf
                     :conn           conn
                     :subscriptions  {}
                     :connected-uids {}})})))

(defn start-percolator
  "register percolation search with ID based on hash of the query"
  [{:keys [current-state msg-payload msg-meta]}]
  (let [query (:query msg-payload)
        sha (p/sha1 (str query))
        uid (:sente-uid msg-meta)]
    (perc/register-query (:conn current-state) "percolator" sha :query query)
    {:new-state (assoc-in current-state [:subscriptions uid] sha)}))

(defn deliver-perc-matches
  "Deliver percolation matches to interested clients by looking clients up in subscriptions and connected-uids."
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [tweet matches]} msg-payload
        connected-uids (:connected-uids @cmp-state)
        subscriptions (:subscriptions @cmp-state)]
    (doseq [uid (:any connected-uids)]
      (when (contains? matches (get subscriptions uid))
        (put-fn (with-meta [:tweet/new tweet] {:sente-uid uid}))))))

(defn count-users
  "Count the number of currently connected client (for display in UI)."
  [{:keys [current-state]}]
  (let [connected-uids (:connected-uids current-state)]
    {:emit-msg (with-meta [:stats/users-count (count (:any connected-uids))] {:sente-uid :broadcast})}))

(defn cmp-map
  "Create component for starting percolation in ElasticSearch and delivering matches."
  [cmp-id conf] {:cmp-id            cmp-id
                 :state-fn          (percolator-state-fn conf)
                 :handler-map       {:perc/matches         deliver-perc-matches
                                     :cmd/percolate        start-percolator
                                     :schedule/count-users count-users}
                 :state-pub-handler (hu/assoc-in-cmp [:connected-uids])})
