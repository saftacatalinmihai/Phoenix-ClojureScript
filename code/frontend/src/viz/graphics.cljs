(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [reagent.core :as r]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defonce state
  (atom
    {:actor-types    {"GenServer" {:type "GenServer" :x 50 :y 50 :color 0xFF00BB}
                      "Actor1"    {:type "Actor1" :x 50 :y 100 :color 0xFFFFBB}
                      "Actor2"    {:type "Actor2" :x 50 :y 150 :color 0x0000BB}}
     :running-actors {"pid1" (atom {:pid "pid1" :x 100 :y 0 :color 0xFF00BB :type "GetServer"})
                      "pid2" (atom {:pid "pid2" :x 200 :y 100 :color 0xFFFFBB :type "Actor1"})
                      "pid3" (atom {:pid "pid3" :x 300 :y 200 :color 0x0000BB :type "Actor2"})}}))

(defn onDragStart[e]
  (this-as this
           (do
             (set! (.-data this) (.-data e))
             (set! (.-alpha this) 0.5)
             (set! (.-dragging this) true))))

(defn onDragEnd[]
  (this-as this
           (do
             (set! (.-alpha this) 1)
             (set! (.-dragging this) false))))

(defn onDragMove[this on-pos-changed]
  (if (.-dragging this)
      (let [newP (.getLocalPosition (.-data this) (.-parent this))]
        (set! (.-x this) (.-x newP))
        (set! (.-y this) (.-y newP))
        (on-pos-changed {:x (.-x newP) :y (.-y newP)}))))

(defn update-running-actor-xy-state[state {new-x :x new-y :y}]
  (swap! state
         #(-> %
           (assoc-in
             [:x]
             new-x)
           (assoc-in
             [:y]
             new-y))))

(defn circle-sprite[app {x :x y :y c :color}]
  (let [graphics (-> (new js/PIXI.Graphics)
                     (.lineStyle 5 c 1)
                     (.beginFill c 0.6)
                     (.drawCircle x y 50)
                     (.endFill))]
    (set! (.-boundsPadding graphics) 0)
    (let [sprite (new js/PIXI.Sprite (.generateTexture graphics))]
      (set! (.-interactive sprite) true)
      (set! (.-buttonMode sprite) true)
      (.anchor.set sprite 0.5)
      (set! (.-x sprite) x)
      (set! (.-y sprite) y)
      (set! (.-color sprite) c)
      sprite)))

(defn running-actor[app init-state]
  (let [running-actor-state (atom init-state)
        actor_sprite        (circle-sprite app
                                           {:x     (:x init-state)
                                            :y     (:y init-state)
                                            :color (:color init-state)})]
    (-> actor_sprite
        (.on "pointerdown" onDragStart)
        (.on "pointerup" onDragEnd)
        (.on "pointerupoutside" onDragEnd)
        (.on "pointermove"
             (fn[e]
               (this-as this
                        (onDragMove this (fn[new-xy] (update-running-actor-xy-state running-actor-state new-xy)))))))
    (.stage.addChild app actor_sprite)
    running-actor-state))

(defn init[mount_elem width height]
  (let [app (new js/PIXI.Application width, height, (clj->js {"antialias" true}))]
    (.appendChild mount_elem (.-view app))

    (let [separator_line_left (new js/PIXI.Graphics)]
      (-> separator_line_left
          (.beginFill 0)
          (.lineStyle 10 0xf44242 0.7)
          (.moveTo 150 0)
          (.lineTo 150 height)
          (.endFill))
      (.stage.addChild app separator_line_left))

    (doseq [[pid existing-state] (:running-actors @state)]
      (swap! state assoc-in [:running-actors pid] (running-actor app @existing-state)))

    (let [event-channel (chan)]
      (let [handlers {:new_running_actor (fn [{pid "pid" name "name"}]
                                           (swap! state assoc-in [:running-actors pid]
                                                  (running-actor app {:pid pid :x 500 :y 400 :color 0x41f447 :type name})))}]
        (go
          (while true
                 (let [[event-name event-data] (<! event-channel)]
                   ((event-name handlers) event-data))))

        event-channel))))