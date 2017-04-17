(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   cljsjs.pixi
   [cljs.core.async :refer [put! chan <! >! timeout close!]]
   [viz.animation :as anim]))

(def component-size 50)

(defn rand-color[]
  (rand-int 0xFFFFFF))

(defonce state
  (atom {
         :actor-types-number 0
         ;; :component (atom {:x 200 :y 200 :color (rand-color)})
         }))

(defn on-top-of-component? [{x :x y :y} component-state]
  (and
   (< (Math.abs (- (component-state :x) x)) component-size)
   (< (Math.abs (- (component-state :y) y)) component-size)
   ))

(defn running-actors-under [{x :x y :y}]
  (filter (fn [[k v]]
            (on-top-of-component? {:x x :y y} @v))
          (@state :running-actors)))


(defn onDragStart[e]
  (this-as this
           (do
             ;; (js/console.log "d s" (pr-str this))
             (set! (.-data this) (.-data e))
             (set! (.-alpha this) 0.5)
             (set! (.-dragging this) true))))

(defn onDragEnd[e]
  (this-as this
           (do
             ;; (js/console.log "d e" (pr-str this))
             (set! (.-alpha this) 1)
             (set! (.-dragging this) false))))

(defn onDragMove[e]
  (this-as this
           (do
             ;; (js/console.log "d m" (pr-str this))
             (if (.-dragging this)
               (let [newP (.getLocalPosition (.-data this) (.-parent this))]
                 (put! (.-eventChan this) [:update-xy {:x (.-x newP) :y (.-y newP)}]))
               )
             )))

(defn draggable
  ([component]
   (-> component
       (.on "pointerdown" onDragStart)
       (.on "pointerup" onDragEnd)
       (.on "pointermove" onDragMove)))
  ([component on-release]
   (-> component
       (.on "pointerup" (fn [e]
                          (this-as this
                                   (do
                                     (set! (.-alpha this) 1)
                                     (set! (.-dragging this) false)
                                     (let [newP (.getLocalPosition (.-data this) (.-parent this))]
                                       (on-release {:x (.-x newP) :y (.-y newP)}))))))
       (.on "pointerdown" onDragStart)
       (.on "pointermove" onDragMove))))

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
                     (shape-constructor x y component-size)
                     (.endFill))]
    (set! (.-boundsPadding graphics) 0)
    (let [sprite (js/PIXI.Sprite. (.generateCanvasTexture graphics))]
      (set! (.-interactive sprite) true)
      (set! (.-buttonMode sprite) true)
      (.anchor.set sprite 0.5)
      (set! (.-x sprite) x)
      (set! (.-y sprite) y)
      (set! (.-color sprite) c)
      sprite)))

(defn background-sprite [w h]
  (let [graphics (-> (js/PIXI.Graphics.)
                     (.beginFill 0x000000)
                     (.drawRect 0 0 w h)
                     (.endFill))
        sprite (js/PIXI.Sprite. (.generateCanvasTexture graphics))]
    (set! (.-interactive sprite) true)
    sprite))

(defn thick-border [graphics color]
  (.lineStyle graphics 5 color 1))

(defn circle-sprite[{x :x y :y c :color}]
  (shape-sprite #( -> %1 (thick-border c) (.drawCircle %2 %3 %4)) {:x x :y y :color c}))

(defn rect-sprite[{x :x y :y c :color}]
  (shape-sprite #(.drawRect %1 %2 %3 (* 1.5 %4) (* 1.5 %4)) {:x x :y y :color c}))

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
        (clickable #(put! (:core-chan @state) [:running-actor-click (deref (.-state running-actor-sprite))])))))

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
        (clickable #(put! (:core-chan @state) [:show-code (:type (deref (.-state actor-type-sprite)))]))
        (draggable)
        )
    actor-type-sprite))

(defn message-component [init-state]
  (let [message-sprite (js/PIXI.Sprite.fromImage "imgs/message-icon-png-14.png")]
    (.scale.set message-sprite 0.1)
    (.anchor.set message-sprite 0.5)
    (set! (.-graphicsChannel message-sprite) (:graphics-channel init-state))
    (set! (.-interactive message-sprite) true)
    (set! (.-buttonMode message-sprite) true)
    (set! (.-x message-sprite) (:x init-state))
    (set! (.-y message-sprite) (:y init-state))
    (set! (.-msg message-sprite) (:msg init-state))
    (clickable message-sprite
               #(put!
                 (:core-chan @state)
                 [:message-click
                  {:msg (.-msg message-sprite)
                   :x (.-x message-sprite)
                   :y (.-y message-sprite)}]))
    (draggable message-sprite
               #(let [actors-under (running-actors-under %)]
                  (js/console.log actors-under)
                  (js/console.log (empty? actors-under))
                  (if (not (empty? actors-under))
                    (do
                      (js/console.log (pr-str (key (first actors-under))))
                      (js/console.log (pr-str (.-msg message-sprite)))
                      (js/console.log (.-eventChan message-sprite))
                      (put! (:core-chan @state) [:send-actor-message2 {:to (key (first actors-under)) :msg (.-msg message-sprite)}])
                      (put!
                       (init-state :graphics-channel)
                       [:animation [message-sprite
                                    {:anim-function (anim/move-decelerated 0.05)
                                     :from %
                                     :to {:x (.-initialX message-sprite) :y (.-initialY message-sprite)}}]]))
                    )
                  )
               )
    ))

(defn component[sprite-constructor state-atom]
  (let [component     (sprite-constructor @state-atom)
        event-chan (chan)
        handlers   {:update-xy (fn[{x :x y :y}] (swap! state-atom #(-> % (assoc :x x :y y))))}]
    (set! (.-state component) state-atom)
    (set! (.-eventChan component) event-chan)
    (set! (.-eventHandlers component) handlers)
    (add-watch state-atom :component
               (fn [key atom old-state {x :x y :y color :color}]
                 (set! (.-x component) x)
                 (set! (.-y component) y)
                 (set! (.-color component) color)))
    (go
      (while true
        (let [[event-name event-data] (<! event-chan)]
          ((event-name handlers) event-data))))
    component))

(defn init[core-chan mount_elem width height]
  (js/console.log "Existing state:", (pr-str state))
  (swap! state assoc-in [:core-chan] core-chan)
  (let [app (js/PIXI.Application. width, height, (clj->js {"antialias" true}))]
    (.appendChild mount_elem (.-view app))

    (let [background-sprite (background-sprite width height)]
      (.stage.addChild app background-sprite)
      (.on background-sprite "pointerdown"
           (fn [e]
             (put! core-chan [:canvas-click {:x  (.-data.originalEvent.pageX e) :y (.-data.originalEvent.pageY e)}])
             )))

    (.ticker.add app (fn [_]
                       (doseq [[c a] (:animations @state)]
                         (anim/next-frame c a state)
                         )))

    (doseq [[pid running_actor_state] (:running-actors @state)]
      (->> (component running-actor running_actor_state)
           (.stage.addChild app)))

    (doseq [[type actor-type-state] (:actor-types @state)]
      (->> (component actor-type actor-type-state)
           (.stage.addChild app)))

    (doseq [[msg msg_state] (:messages @state)]
      (->> (component message-component msg_state)
           (.stage.addChild app)))

    (let [event-channel (chan)]
      (let [handlers {:new_message (fn [{msg :msg x :x y :y}]
                                     (let [msg-state (atom {:x x :y y :msg msg :graphics-channel event-channel})
                                           m (component message-component msg-state)]
                                       (swap! state assoc-in [:messages msg] msg-state)
                                       (.stage.addChild app m)))
                      :remove_message (fn [msg]
                                        (swap! state update-in [:messages] dissoc msg)
                                        )
                      :new_running_actor (fn [[{pid "pid" name "name"} {x :x y :y c :color}]]
                                           (let [running-actor-state (atom {:pid pid :x x :y y :color c :type name})]
                                             (swap! state assoc-in [:running-actors pid] running-actor-state)
                                             (->> (component running-actor (get-in @state [:running-actors pid]))
                                                  (.stage.addChild app))))
                      :new_actor_type (fn [actor_type]
                                        (swap! state assoc-in [:actor-types actor_type]
                                               (atom {:type actor_type
                                                      :x 60
                                                      :y (+ 60 (* 120 (get-in @state [:actor-types-number])))
                                                      :color (rand-color)}))
                                        (swap! state update-in [:actor-types-number] inc)
                                        (->> (component actor-type (get-in @state [:actor-types actor_type]))
                                             (.stage.addChild app)))
                      :set_actor_types   (fn [actor_types]
                                           (dorun
                                            (map-indexed
                                             (fn [idx type]
                                               (swap! state assoc-in [:actor-types type]
                                                      (atom {:type type :x 60 :y (+ 60 (* 120 idx)) :color (rand-color)}))
                                               (->> (component actor-type (get-in @state [:actor-types type]))
                                                    (.stage.addChild app)))
                                             actor_types))
                                           (swap! state assoc-in [:actor-types-number] (count actor_types)))
                      :animation (fn [[component animation]]
                                   (swap! state assoc-in [:animations component] animation))
                      }]
        (go
          (while true
            (let [[event-name event-data] (<! event-channel)]
              ((event-name handlers) event-data))))

        ;; (put! event-channel [:animation [m {
        ;;                                     :anim-function (anim/move-decelerated 0.05)
        ;;                                     :from {:x 500 :y 200}
        ;;                                     :to {:x 400 :y 300}}]])
        event-channel))))

(defn as-js-setter [keyword] (str ".-" (name keyword)))
(defn set-pixi-attr
  [sprite attr value]
  (aset sprite (name attr) value)
  )
(defn new-sprite [sprite-constructor init-state]
  (let [ch (chan)
        state-atom (r/atom init-state)
        sprite (sprite-constructor init-state)
        handlers {:update-state (fn [new-state]
                                  (swap! state-atom
                                         (fn [old-state]
                                           (reduce
                                            (fn [st [k v]] (assoc st k v))
                                            old-state
                                            new-state))))}]
    (go
      (while true
        (let [[ev-name ev-data] (<! ch)]
          (ev-name handlers) ev-data)))
    (add-watch state-atom :sprite-watch
               (fn [key atom old-state new-state]
                 (doseq [[k v] new-state]
                   (set-pixi-attr sprite k v))))
    {:channel ch
     :state-atom state-atom}
    )
  )
