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

(graphics/init
  (js/document.querySelector "#pixi-js")
  (-> js/window js/jQuery .width) (-> js/window js/jQuery .height))

(channel/join (fn [actor_types]
                 (js/console.log actor_types)))

(channel/push "new_actor" {:name "Actor2"} (fn [running_actor]
                                             (println running_actor)
                                             (put! graphics/EVENTCHANNEL [:new_running_actor running_actor])))