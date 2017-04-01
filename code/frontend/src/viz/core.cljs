(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [dommy.core :as dommy
     :refer-macros  [sel sel1]]
    [reagent.core :as r]
    [viz.channel :as channel]
    [viz.graphics :as graphics]
    [cljs.core.async :refer [put! chan <!]]))

(defonce actors
  (atom {
            "pid1" (atom {:x 0 :y 0 :color 0xFF00BB})
            "pid2" (atom {:x 200 :y 200 :color 0xFFFFBB})
            "pid3" (atom {:x 400 :y 400 :color 0x0000BB})
            }))

(def EVENTCHANNEL (chan))

(def EVENTS
  {:update-actor-state (fn [{pid :pid state :state}]
                         (swap! (get @actors pid) (fn[_] state)))})
(go
  (while true
         (let [[event-name event-data] (<! EVENTCHANNEL)]
           ((event-name EVENTS) event-data))))

(defn pixi []
  [:div {:id "pixi-js"}])

(r/render [pixi]
  (js/document.querySelector "#pixi-mount"))

(def app (graphics/init
  actors
  EVENTCHANNEL
  (js/document.querySelector "#pixi-js")
  (-> js/window js/jQuery .width) (-> js/window js/jQuery .height)))