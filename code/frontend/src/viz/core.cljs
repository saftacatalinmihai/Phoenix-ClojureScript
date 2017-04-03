(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [dommy.core :as dommy
     :refer-macros  [sel sel1]]
    [reagent.core :as r]
    [viz.channel :as channel]
    [viz.graphics :as graphics]
    [cljs.core.async :refer [put! chan <!]]))

(defn pixi []
  [:div {:id "pixi-js"}])

(r/render [pixi]
  (js/document.querySelector "#pixi-mount"))

(def core-chan (chan))

(def graphics-event-chan
  (graphics/init
    core-chan
    (js/document.querySelector "#pixi-js")
    (-> js/window js/jQuery .width) (-> js/window js/jQuery .height)))

(def handlers
  {:start-new-actor (fn[new-actor]
                      (channel/push "start_actor" {:type (:type new-actor)}
                                    (fn [running_actor]
                                      (put! graphics-event-chan [:new_running_actor [running_actor new-actor]]))))})
(go
  (while true
         (let [[event-name event-data] (<! core-chan)]
           (js/console.log (pr-str event-name event-data))
           ((event-name handlers) event-data))))

(defonce joined-chan
  (channel/join
    (fn [actor_types]
      (put! graphics-event-chan [:set_actor_types (get actor_types "actors")]))))