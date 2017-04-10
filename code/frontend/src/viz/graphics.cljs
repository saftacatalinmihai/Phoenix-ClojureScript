(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   cljsjs.pixi
   [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn rand-color[]
  (rand-int 0xFFFFFF))

(defonce state
  (atom {
         :actor-types-number 0
         :component (atom {:x 200 :y 200 :color (rand-color)})}))

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

(defn background-sprite [w h]
  (let [graphics (-> (js/PIXI.Graphics.)
                     (.beginFill 0x000000)
                     (.drawRect 0 0 w h)
                     (.endFill))]
    (set! (.-boundsPadding graphics) 0)
    (let [sprite (js/PIXI.Sprite. (.generateTexture graphics))]
      (set! (.-interactive sprite) true)
      sprite
      ))
  )

(defn thick-border [graphics color]
  (.lineStyle graphics 5 color 1))

(defn circle-sprite[{x :x y :y c :color}]
  (shape-sprite #( -> %1 (thick-border c) (.drawCircle %2 %3 %4)) {:x x :y y :color c}))

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
        (draggable)
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

(defn message-component [init-state]
  (let [message-sprite (js/PIXI.Sprite.fromImage "imgs/message-icon-png-14.png")]
    (.scale.set message-sprite 0.1)    
    (.anchor.set message-sprite 0.5)
    (set! (.-interactive message-sprite) true)
    (set! (.-buttonMode message-sprite) true)
    (set! (.-x message-sprite) (:x init-state))
    (set! (.-y message-sprite) (:y init-state))
    (draggable message-sprite)
    (clickable message-sprite #(put! (:core-chan @state) [:message-click {:x (.-x message-sprite ) :y (.-y message-sprite) }])))
)

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

    (let [background-sprite (background-sprite width height)]
      (.stage.addChild app background-sprite)
      (.on background-sprite "pointerdown" (fn [e] 
                                        (js/console.log (.-data.originalEvent.pageX e) , (.-data.originalEvent.pageY e))
                                        (put! core-chan [:canvas-click {:x  (.-data.originalEvent.pageX e) :y (.-data.originalEvent.pageY e)}])
                                        ))
      )

    (def m (component message-component (atom {:x 200 :y 100})))
    (.stage.addChild app m)

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
                      :new_actor_type (fn [actor_type]
                                        (swap! state assoc-in [:actor-types actor_type]
                                               (atom {:type actor_type :x 60 :y (+ 200 (* 120 (get-in @state [:actor-types-number]))) :color (rand-color)}))
                                        (swap! state update-in [:actor-types-number] inc)
                                        (->> (component actor-type (get-in @state [:actor-types actor_type]))
                                             (.stage.addChild app)))
                      :set_actor_types   (fn [actor_types]
                                           (dorun
                                            (map-indexed
                                             (fn [idx type]
                                               (swap! state assoc-in [:actor-types type]
                                                      (atom {:type type :x 60 :y (+ 200 (* 120 idx)) :color (rand-color)}))
                                               (->> (component actor-type (get-in @state [:actor-types type]))
                                                    (.stage.addChild app)))
                                             actor_types))
                                           (swap! state assoc-in [:actor-types-number] (count actor_types))
                                           )}]
        (go
          (while true
            (let [[event-name event-data] (<! event-channel)]
              ((event-name handlers) event-data))))

        event-channel))))
