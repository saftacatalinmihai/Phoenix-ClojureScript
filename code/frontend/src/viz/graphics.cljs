(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [reagent.core :as r]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defonce state
  (atom {}))

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

(defn update-xy-state[state {new-x :x new-y :y}]
  (swap! state
         #(-> %
           (assoc-in
             [:x]
             new-x)
           (assoc-in
             [:y]
             new-y))))

(defn dragable-sprite
  ([sprite] (dragable-sprite sprite identity))
  ([sprite onMove]
    (-> sprite
        (.on "pointerdown" onDragStart)
        (.on "pointerup" onDragEnd)
        (.on "pointerupoutside" onDragEnd)
        (.on "pointermove"
             (fn[e]
               (this-as this
                        (onDragMove this onMove)))))))

(defn circle-sprite[app {x :x y :y c :color}]
  (let [graphics (-> (js/PIXI.Graphics.)
                     (.lineStyle 5 c 1)
                     (.beginFill c 0.6)
                     (.drawCircle x y 50)
                     (.endFill))]
    (set! (.-boundsPadding graphics) 0)
    (let [sprite (js/PIXI.Sprite. (.generateTexture graphics))]
      (set! (.-interactive sprite) true)
      (set! (.-buttonMode sprite) true)
      (.anchor.set sprite 0.5)
      (set! (.-x sprite) x)
      (set! (.-y sprite) y)
      (set! (.-color sprite) c)
      sprite)))

(defn running-actor[app init-state]
  (let [running-actor-state   (atom init-state)
        running-actor-sprite  (dragable-sprite
                                (circle-sprite app
                                               {:x     (:x init-state)
                                                :y     (:y init-state)
                                                :color (:color init-state)})
                                (fn[new-xy] (update-xy-state running-actor-state new-xy)))]
    (let [pid-text (js/PIXI.Text. (:pid @running-actor-state)
                                (clj->js
                                  {:fill            "white"
                                   :fontSize        16}))]
      (set! (.-anchor.x pid-text) 0.5)
      (set! (.-anchor.y pid-text) 0.5)
      (.addChild running-actor-sprite pid-text))
    (.stage.addChild app running-actor-sprite)
    running-actor-state))

(defn actor-type[app init-state]
  (let [actor-type-state  (atom init-state)
        actor-type-sprite (circle-sprite app
                                         {:x     (:x init-state)
                                          :y     (:y init-state)
                                          :color (:color init-state)})]
    (let [text (js/PIXI.Text. (:type init-state))]
      (set! (.-anchor.x text) 0.5)
      (set! (.-anchor.y text) 0.5)
      (.addChild actor-type-sprite text))

    (let [onDragEnd (fn[]
                      (this-as this
                               (do
                                 (put! (:core-chan @state) [:start-new-actor @actor-type-state])
                                 (set! (.-x this) (.-initialX this))
                                 (set! (.-y this) (.-initialY this))
                                 (update-xy-state actor-type-state {:x (.-x this) :y (.-y this)})
                                 (set! (.-alpha this) 1)
                                 (set! (.-dragging this) false))))]
      (-> actor-type-sprite
          (.on "pointerdown"
               (fn [e]
                 (this-as this
                          (do
                            (set! (.-initialX this) (.-x this))
                            (set! (.-initialY this) (.-y this))
                            (set! (.-data this) (.-data e))
                            (set! (.-alpha this) 0.5)
                            (set! (.-dragging this) true)))))
          (.on "pointerup" onDragEnd)
          (.on "pointerupoutside" onDragEnd)
          (.on "pointermove"
               (fn[e]
                 (this-as this
                          (onDragMove this (fn[new-xy] (update-xy-state actor-type-state new-xy))))))))
    (.stage.addChild app actor-type-sprite)
    actor-type-state))

(defn rand-color[]
  (rand-int 0xFFFFFF))

(defn init[core-chan mount_elem width height]
  (js/console.log "Existing state:", (pr-str state))
  (swap! state assoc-in [:core-chan] core-chan)
  (let [app (js/PIXI.Application. width, height, (clj->js {"antialias" true}))]
    (.appendChild mount_elem (.-view app))

    (dorun
      (map-indexed
        (fn [idx [type type-state]]
          (swap! state assoc-in [:actor-types type]
                 (actor-type app
                             {:type type :x (:x @type-state) :y (:y @type-state) :color (:color @type-state)})))
        (:actor-types @state)))

    (doseq [[pid existing-state] (:running-actors @state)]
      (swap! state assoc-in [:running-actors pid] (running-actor app @existing-state)))

    (let [event-channel (chan)]
      (let [handlers {:new_running_actor (fn [[{pid "pid" name "name"} {x :x y :y c :color}]]
                                           (println pid, name, x, y, c)
                                           (swap! state assoc-in [:running-actors pid]
                                                  (running-actor app {:pid pid :x x :y y :color c :type name})))
                      :set_actor_types   (fn [actor_types]
                                           (dorun
                                             (map-indexed
                                               (fn [idx type]
                                                 (swap! state assoc-in [:actor-types type]
                                                        (actor-type app {:type type :x 100 :y (+ 50 (* 120 idx)) :color (rand-color)})))
                                               actor_types)))}]
        (go
          (while true
                 (let [[event-name event-data] (<! event-channel)]
                   ((event-name handlers) event-data))))

        event-channel))))