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
                         (swap! (get @actors pid) (fn[_] state)))
   :new_running_actor  (fn [{pid "pid" name "name"}]
                        (swap! actors assoc-in [pid] (atom {:x 500 :y 400 :color 0x41f447 :name name})))})
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

(channel/join (fn [actor_types]
                 (js/console.log actor_types)))

(channel/push "new_actor" {:name "Actor2"} (fn [running_actor]
                                             (println running_actor)
                                             (put! EVENTCHANNEL [:new_running_actor running_actor])))