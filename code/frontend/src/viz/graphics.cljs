(ns viz.graphics
  (:require
    [reagent.core :as r]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

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

(defn onDragMove[e]
  (this-as this
           (if (.-dragging this)
               (let [newP (.getLocalPosition (.-data this) (.-parent this))]
                 (put! (.-eventChannel this)
                       [:update-actor-state {:pid (.-pid this) :state {:x (.-x newP) :y (.-y newP) :color (.-color this)}}])))))

(defn create_actor[app EVENTCHANNEL x y color pid]
  (def actor_graphics (new js/PIXI.Graphics))
  (-> actor_graphics
      (.lineStyle 4 color 1)
      (.beginFill color 0.6)
      (.drawCircle x y 50)
      (.endFill))

  (set! (.-boundsPadding actor_graphics) 0)

  (def actor_sprite (new js/PIXI.Sprite (.generateTexture actor_graphics)))

  (set! (.-interactive actor_sprite) true)
  (set! (.-buttonMode actor_sprite) true)
  (.anchor.set actor_sprite 0.5)

  (set! (.-x actor_sprite) x)
  (set! (.-y actor_sprite) y)
  (set! (.-color actor_sprite) color)
  (set! (.-eventChannel actor_sprite) EVENTCHANNEL)
  (set! (.-pid actor_sprite) pid)

  (-> actor_sprite
      (.on "pointerdown" onDragStart)
      (.on "pointerup" onDragEnd)
      (.on "pointerupoutside" onDragEnd)
      (.on "pointermove" onDragMove))

  (.stage.addChild app actor_sprite)
  actor_sprite)

(defn init[actors EVCHANNEL mount_elem width height]
  (def app (new js/PIXI.Application width, height, (clj->js {"antialias" true})))
  (.appendChild mount_elem (.-view app))

  (def separator_line_left (new js/PIXI.Graphics))
  (-> separator_line_left
      (.beginFill 0)
      (.lineStyle 5 0xf44242 0.7)
      (.moveTo 150 0)
      (.lineTo 150 height)
      (.endFill))
  (.stage.addChild app separator_line_left)

  (doseq [actor @actors]
    (let [[pid state]          actor
          {x :x y :y c :color} @state]
      (let [actor_sprite (create_actor app EVCHANNEL x y c pid)]
        (add-watch state pid
                   (fn[key atom old-state new-state]
                     (set! (.-x actor_sprite) (:x new-state))
                     (set! (.-y actor_sprite) (:y new-state))
                     (set! (.-color actor_sprite) (:color new-state)))))))
  app)

(defn add_actor_on_stage [app EVENTCHANNEL x y c pid]
  (create_actor app EVENTCHANNEL x y c pid))