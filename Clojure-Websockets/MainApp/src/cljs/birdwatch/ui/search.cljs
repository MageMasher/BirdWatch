(ns birdwatch.ui.search
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]))

(def s [:svg {:height "1em" :viewBox "0 0 1200 1200"}
            [:g {:transform "rotate(90 600 600)"}
             [:path {:fill :white :d "M23 693q0 200 142 342t342 142t342 -142t142 -342q0 -142 -78 -261l300 -300q7 -8 7 -18t-7 -18l-109 -109q-8 -7 -18 -7t-18 7l-300 300q-119 -78 -261 -78q-200 0 -342 142t-142 342zM176 693q0 -136 97 -233t234 -97t233.5 96.5t96.5 233.5t-96.5 233.5t-233.5 96.5 t-234 -97t-97 -233z"}]]])

(defn- search-view
  [{:keys [put-fn]}]
       [:div
        [:form.pure-form
         [:fieldset
          [:input {:type         "text"
                   :on-key-press #(when (== (.-keyCode %) 13) (put-fn [:cmd/start-search]))
                   :on-change    #(put-fn [:cmd/set-search-text (.. % -target -value)])
                   :placeholder  "Example search: java (job OR jobs OR hiring)"}]
          [:button.pure-button.pure-button-primary {:on-click #(put-fn [:cmd/start-search])}
           [:span s]]]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn search-view
              :dom-id  "search"}))
