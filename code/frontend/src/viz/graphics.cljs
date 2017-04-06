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

(defn draggable[component]
  (-> component
      (.on "pointerdown" onDragStart)
      (.on "pointerup" onDragEnd)
      (.on "pointermove" onDragMove)))

(defn clickable [component cb]
  (-> component
      (.on "pointerdown" (fn [e]
                           (this-as this
                                    (do
                                      (set! (.-initialX this) (.-x this))
                                      (set! (.-initialY this) (.-y this))))))
      (.on "pointerup" (fn[e]
                         (this-as this
                                  (if (and (= (.-x this) (.-initialX this)) (= (.-y this) (.-initialY this)))
                                    (cb {:x (.-x this) :y (.-y this)})))))))

(defn shape-sprite [shape-constructor {x :x y :y c :color}]
  (let [graphics (-> (js/PIXI.Graphics.)
                     (.lineStyle 5 c 1)
                     (.beginFill c 0.6)
                     (shape-constructor x y 50)
                     (.endFill))]
    (set! (.-boundsPadding graphics) 0)
    (let [sprite (js/PIXI.Sprite. (.generateTexture graphics))]
      (set! (.-interactive sprite) true)
      (set! (.-buttonMode sprite) true)
      (.anchor.set sprite 0.5)
      (set! (.-x sprite) x)
      (set! (.-y sprite) y)
      (set! (.-color sprite) c)
      sprite
      )))

(defn circle-sprite[{x :x y :y c :color}]
  (shape-sprite #(.drawCircle %1 %2 %3 %4) {:x x :y y :color c}))

(defn rect-sprite[{x :x y :y c :color}]
  (shape-sprite #(.drawRect %1 %2 %3 (* 1.5 %4) (* 1.5 %4)) {:x x :y y :color c}))

(defn add-actor [init-state]
  (let [add-actor-sprite (rect-sprite 
                          {:x (:x init-state)
                           :y (:y init-state)
                           :color (:color init-state)})]

    (let [plus-text (js/PIXI.Text. "+" (clj->js {:fill "white" :fontSize 50}))]
      (set! (.-anchor.x plus-text) 0.5)
      (set! (.-anchor.y plus-text) 0.5)
      (.addChild add-actor-sprite plus-text))
    
    (-> add-actor-sprite
        (clickable #(put! (:core-chan @state) [:add-new-actor :ok])))
))

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
      (.addChild running-actor-sprite pid-text))

    (-> running-actor-sprite
        (draggable)
        (clickable #(put! (:core-chan @state) [:open-message-modal (deref (.-state running-actor-sprite))]))
)))

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

    (-> actor-type-sprite
        (.on "pointerup" (fn[]
                           (this-as this
                                    (let [s (.-state this)]
                                      (if-not (and (= (.-x this) (.-initialX this)) (= (.-y this) (.-initialY this)))
                                        (put! (:core-chan @state) [:start-new-actor @s]))
                                      (put! (.-eventChan this) [:update-xy {:x (.-initialX this)  :y (.-initialY this)}])
                                      (set! (.-alpha this) 1)
                                      (set! (.-dragging this) false)))) )
        (draggable)
        (clickable #(put! (:core-chan @state) [:show-code (:type (deref (.-state actor-type-sprite)))])))
    actor-type-sprite
    ))

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

(defn init[core-chan mount_elem width height]
  (js/console.log "Existing state:", (pr-str state))
  (swap! state assoc-in [:core-chan] core-chan)
  (let [app (js/PIXI.Application. width, height, (clj->js {"antialias" true}))]
    (.appendChild mount_elem (.-view app))

    (doseq [[pid running_actor_state] (:running-actors @state)]
      (->> (component running-actor running_actor_state)
           (.stage.addChild app)))

    (doseq [[type actor-type-state] (:actor-types @state)]
      (->> (component actor-type actor-type-state)
           (.stage.addChild app)))

    (->> (component add-actor (atom {:x 60 :y 70 :color 0x2ABF3B}))
         (.stage.addChild app))

    (let [event-channel (chan)]
      (let [handlers {:new_running_actor (fn [[{pid "pid" name "name"} {x :x y :y c :color}]]
                                           (let [running-actor-state (atom {:pid pid :x x :y y :color c :type name})]
                                             (swap! state assoc-in [:running-actors pid] running-actor-state)
                                             (->> (component running-actor (get-in @state [:running-actors pid]))
                                                  (.stage.addChild app))))
                      :set_actor_types   (fn [actor_types]
                                           (dorun
                                            (map-indexed
                                             (fn [idx type]
                                               (swap! state assoc-in [:actor-types type]
                                                      (atom {:type type :x 60 :y (+ 200 (* 120 idx)) :color (rand-color)}))
                                               (->> (component actor-type (get-in @state [:actor-types type]))
                                                    (.stage.addChild app)))
                                             actor_types)))}]
        (go
          (while true
            (let [[event-name event-data] (<! event-channel)]
              ((event-name handlers) event-data))))

        event-channel))))
