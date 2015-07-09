(ns ^:figwheel-no-load birdwatch.dev
  (:require [birdwatch.core :as c]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(defn jscb [] 
  (c/init))

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3452/figwheel-ws"
  :jsload-callback jscb)

(c/init)
