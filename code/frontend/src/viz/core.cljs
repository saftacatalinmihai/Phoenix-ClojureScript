(ns viz.core
  (:require
    [dommy.core :as dommy
     :refer-macros  [sel sel1]]
    [reagent.core :as r]
    [viz.channel :as channel]
    [viz.graphics :as graphics]))

(defn pixi []
  [:div {:id "pixi-js"}])

(r/render [pixi]
            (js/document.querySelector "#pixi-mount"))

(graphics/init
  (js/document.querySelector "#pixi-js")
  (-> js/window js/jQuery .width (#(* % (/ 5 10)))) 600)
