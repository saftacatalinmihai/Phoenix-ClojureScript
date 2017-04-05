(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn rand-color[]
  (rand-int 0xFFFFFF))

(defonce state
  (atom {:component (atom {:x 200 :y 200 :color (rand-color)})}))

(defn onDragStart[e]
  (this-as this
           (do
             (set! (.-data this) (.-data e))
             (set! (.-alpha this) 0.5)
             (set! (.-dragging this) true))))

(defn onDragEnd[e]
  (this-as this
           (do
             (set! (.-alpha this) 1)
             (set! (.-dragging this) false))))

(defn onDragMove[e]
  (this-as this
           (if (.-dragging this)
             (let [newP (.getLocalPosition (.-data this) (.-parent this))]
               (put! (.-eventChan this) [:update-xy {:x (.-x newP) :y (.-y newP)}])))))

(defn circle-sprite[{x :x y :y c :color}]
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

(defn running-actor[init-state]
  (let [running-actor-sprite  (circle-sprite
                               {:x     (:x init-state)
                                :y     (:y init-state)
                                :color (:color init-state)})]
    (let [pid-text (js/PIXI.Text. (:pid init-state)
                                  (clj->js
                                   {:fill "white" :fontSize 16}))]
      (set! (.-anchor.x pid-text) 0.5)
      (set! (.-anchor.y pid-text) 0.5)
      (.addChild running-actor-sprite pid-text)
      )
    running-actor-sprite))

(defn actor-type[init-state]
  (let [actor-type-sprite (circle-sprite
                           {:x     (:x init-state)
                            :y     (:y init-state)
                            :color (:color init-state)})]
    (let [text (js/PIXI.Text. (:type init-state)
                              (clj->js
                               {:fill "white" :fontSize 14}))]
      (set! (.-anchor.x text) 0.5)
      (set! (.-anchor.y text) 0.5)
      (.addChild actor-type-sprite text))

    (let [onDragEnd (fn[]
                      (this-as this
                               (let [s (.-state this)]
                                 (if-not (and (= (.-x this) (.-initialX this)) (= (.-y this) (.-initialY this)))
                                   (put! (:core-chan @state) [:start-new-actor @s])
                                   (put! (:core-chan @state) [:show-code (:type @s)]))
                                 (put! (.-eventChan this) [:update-xy {:x (.-initialX this)  :y (.-initialY this)}])
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
          (.on "pointerup" onDragEnd)))
    actor-type-sprite))

(defn component[sprite-constructor state-atom]
  (let [circle     (sprite-constructor @state-atom)
        event-chan (chan)
        handlers   {:update-xy (fn[{x :x y :y}]
                                 (swap! state-atom
                                        #(-> %
                                             (assoc-in
                                              [:x]
                                              x)
                                             (assoc-in
                                              [:y]
                                              y))))}]
    (set! (.-state circle) state-atom)
    (set! (.-eventChan circle) event-chan)
    (set! (.-eventHandlers circle) handlers)
    (add-watch state-atom :component
               (fn [key atom old-state {x :x y :y color :color}]
                 (set! (.-x circle) x)
                 (set! (.-y circle) y)
                 (set! (.-color circle) color)))
    (go
      (while true
        (let [[event-name event-data] (<! event-chan)]
          ((event-name handlers) event-data))))
    circle))

(defn draggable[component]
  (.on component "pointerdown" onDragStart)
  (.on component "pointerup" onDragEnd)
  (.on component "pointermove" onDragMove))

(defn init[core-chan mount_elem width height]
  (js/console.log "Existing state:", (pr-str state))
  (swap! state assoc-in [:core-chan] core-chan)
  (let [app (js/PIXI.Application. width, height, (clj->js {"antialias" true}))]
    (.appendChild mount_elem (.-view app))

    (doseq [[pid running_actor_state] (:running-actors @state)]
      (->> (component running-actor running_actor_state)
           (.stage.addChild app)
           (draggable)))

    (doseq [[type actor-type-state] (:actor-types @state)]
      (->> (component actor-type actor-type-state)
           (.stage.addChild app)
           (draggable)))

    (let [event-channel (chan)]
      (let [handlers {:new_running_actor (fn [[{pid "pid" name "name"} {x :x y :y c :color}]]
                                           (let [running-actor-state (atom {:pid pid :x x :y y :color c :type name})]
                                             (swap! state assoc-in [:running-actors pid] running-actor-state)
                                             (->> (component running-actor (get-in @state [:running-actors pid]))
                                                  (.stage.addChild app)
                                                  (draggable))
                                             ))
                      :set_actor_types   (fn [actor_types]
                                           (dorun
                                            (map-indexed
                                             (fn [idx type]
                                               (swap! state assoc-in [:actor-types type]
                                                      (atom {:type type :x 60 :y (+ 60 (* 120 idx)) :color (rand-color)}))
                                               (->> (component actor-type (get-in @state [:actor-types type]))
                                                    (.stage.addChild app)
                                                    (draggable)))
                                             actor_types)))}]
        (go
          (while true
            (let [[event-name event-data] (<! event-channel)]
              ((event-name handlers) event-data))))

        event-channel))))
