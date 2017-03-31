(ns viz.graphics)

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
                 (set! (.-x this) (.-x newP))
                 (set! (.-y this) (.-y newP))))))

(defn create_actor[x y]
  (def actor_graphics (new js/PIXI.Graphics))
  (-> actor_graphics
      (.lineStyle 0)
      (.beginFill 0xFF00BB 0.25)
      (.drawCircle x y 100)
      (.endFill))

  (set! (.-boundsPadding actor_graphics) 0)

  (def actor_sprite (new js/PIXI.Sprite (.generateTexture actor_graphics)))

  (set! (.-interactive actor_sprite) true)
  (set! (.-buttonMode actor_sprite) true)
  (.anchor.set actor_sprite 0.5)

  (set! (.-x actor_sprite) x)
  (set! (.-y actor_sprite) y)

  (-> actor_sprite
      (.on "pointerdown" onDragStart)
      (.on "pointerup" onDragEnd)
      (.on "pointerupoutside" onDragEnd)
      (.on "pointermove" onDragMove))

  (.stage.addChild app actor_sprite))

(defn init[mount_elem width height]
  (def app (new js/PIXI.Application width, height, (clj->js {"antialias" true})))
  (.appendChild mount_elem (.-view app))

  (create_actor 0 0)
  (create_actor 50 50)
  (create_actor 200 200))

;
;  (defn draw_actor[{g :actor_graphics f :fill ls :line_style x :x y :y r :r}]
;    (-> g
;        (.lineStyle (:lineWidth ls))
;        (.beginFill (:color f) (:alpha f))
;        (.drawCircle x y r)
;        (.endFill)))
;
;  (draw_actor
;    {:actor_graphics actor_graphics
;     :fill           {:color 0xFF00BB :alpha 0.25}
;     :line_style     {:lineWidth 0}
;     :x              470
;     :y              200
;     :r              60}))