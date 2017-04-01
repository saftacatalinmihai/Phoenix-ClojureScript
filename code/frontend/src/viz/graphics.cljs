(ns viz.graphics
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [reagent.core :as r]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defonce actors
  (atom {
          "pid1" (atom {:x 0 :y 0 :color 0xFF00BB})
          "pid2" (atom {:x 200 :y 200 :color 0xFFFFBB})
          "pid3" (atom {:x 400 :y 400 :color 0x0000BB})
          }))

(def EVENTCHANNEL (chan))

(def EVENTS
  {:update-actor-state (fn [{pid :pid state :state}]
                         (swap! (get @actors pid) (fn[_] state)))
   :new_running_actor  (fn [{pid "pid" name "name"}]
                         (swap! actors assoc-in [pid] (atom {:x 500 :y 400 :color 0x41f447 :name name})))})
(go
  (while true
         (let [[event-name event-data] (<! EVENTCHANNEL)]
           ((event-name EVENTS) event-data))))


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

(defn create_actor[app x y color pid]
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

(defn add_actor_on_stage [app pid actor_state]
  (let [{x :x y :y c :color} @actor_state
        actor_sprite (create_actor app x y c pid)]
    (add-watch actor_state pid
               (fn[key atom old-state new-state]
                 (set! (.-x actor_sprite) (:x new-state))
                 (set! (.-y actor_sprite) (:y new-state))
                 (set! (.-color actor_sprite) (:color new-state))))))

(defn init[mount_elem width height]
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

  (defn add_all_actors_on_stage [actors]
    (doseq [actor actors]
      (let [[pid state]          actor]
        (add_actor_on_stage app pid state))))

  (add-watch actors :graphics
             (fn [key atom old-state new-state]
               (add_all_actors_on_stage new-state)))
  app)

