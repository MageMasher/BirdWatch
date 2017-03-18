(ns birdwatch.ui.tweets
  (:require [birdwatch.ui.util :as util]
            [reagent.core :as r :refer [atom]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [clojure.string :as s]))

(defn twitter-intent
  "Renders a twitter intent as a clickable image, for example for retweeting
   directly from inside the application."
  [tweet intent icon]
  [:a {:href (str "https://twitter.com/intent/" intent (:id_str tweet))}
   [:img {:src (str "/images/" icon)}]])

(defn twitter-intents
  "Renders the three relevant twitter intents using the component above."
  [tweet]
  [:div.intent
   [twitter-intent tweet "tweet?in_reply_to=" "reply.png"]
   [twitter-intent tweet "retweet?tweet_id=" "retweet.png"]
   [twitter-intent tweet "favorite?tweet_id=" "favorite.png"]])

(defn missing-tweet
  "Renders the view for a missing tweet, which in ideal cases should only
   be shown for fractions of a second until the tweet that should have
   been displayed instead is loaded over the WebSockets connection."
  [tweet put-fn]
  (let [id-str (:id_str tweet)]
    (put-fn [:cmd/retrieve-missing id-str])
    [:div.tweet "loading... " (:id_str tweet)]))

(defn tweet-text
  "Renders the text of a tweet including followers count plus retweet,
   favorites and retweeted-within-loaded-tweets count."
  [tweet user]
  (let [app (subscribe [:app])
        rt (reaction (util/rt-count tweet @app))
        fav (reaction (util/fav-count tweet @app))
        rt-total (reaction (util/rt-count-since-startup tweet @app))
        followers (util/number-format (:followers_count user))]
    (fn tweet-text-render [tweet user]
      [:div.tweettext
       [:div {:dangerouslySetInnerHTML #js {:__html (:html-text tweet)}}]
       [:div.pull-left.time-interval (str followers " followers")]
       [:div.pull-right.time-interval (str @rt @fav)
        [:br] @rt-total]])))

(defn http-replacer
  "Function for replacing 'http' with 'https' in image URLs."
  [url]
  (s/replace url "http:" "https:"))

(defn image-view
  "Renders the first image inside the media vector as its only argument.
   The assumption is that the interesting image is always contained at
   that position, which appears to be the case."
  [media]
  [:div.tweet-image
   [:a {:href (:url (get media 0)) :target "_blank"}
    [:img.pure-img-responsive
     {:src (http-replacer (str (:media_url (get media 0)) ":small"))}]]])

(defn tweet-view
  "Renders a tweet with all the elements it contains. Takes the raw (unformatted)
   tweet and the dereferenced application state as arguments."
  [tweet]
  (let [user (:user tweet)
        screen-name (:screen_name user)
        href (str "https://www.twitter.com/" screen-name)]
    [:div.tweet
     [:span
      [:a {:href href :target "_blank"}
       [:img.thumbnail {:src (http-replacer (:profile_image_url user))}]]]
     [:a {:href href :target "_blank"}
      [:span.username {:src (http-replacer (:profile_image_url user))}
       (:name user)]]
     [:span.username_screen (str " @" screen-name)]
     [:div.pull-right.time-interval (util/from-now (:created_at tweet))]
     [tweet-text tweet user]
     (when-let [media (:media (:entities tweet))]
       (pos? (count media)) [image-view media])
     [twitter-intents tweet]]))

(defn tweets-view
  "Renders a list of tweets. Takes atom app plus the cmd-chan
   as arguments. Dereferences both and passes down dereferenced data structures
   so that so that the tweet-view component and all components down the hierarchy
   can be implemented as pure functions.
   Rerenders the entire list whenever one (or both) of the atoms change."
  [put-fn]
  (let [app (subscribe [:app])
        order (reaction (:sorted @app))
        ordered-tweets (reaction (util/tweets-by-order @app))]
    (fn tweet-view-render [put-fn]
      (when @order
        (let [tweets @ordered-tweets]
          [:div (for [t tweets]
                  (if (:user t)
                    ^{:key (:id_str t)} [tweet-view t]
                    ^{:key (:id_str t)} [missing-tweet t put-fn]))])))))