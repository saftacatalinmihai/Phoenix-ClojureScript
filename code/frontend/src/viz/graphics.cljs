(ns viz.graphics
  (:require
    [quil.core :as   q
     :include-macros true]
    [quil.middleware :as m
     :include-macros     true]))

(defn init[canvas width height]
  (println height)
  (println width)
  (def app (new js/PIXI.Application 800, 600, (clj->js { "antialias" true })))
  (.log js/console app)
  (.appendChild (.-body js/document) (-> app .-view))

  (def graphics (new js/PIXI.Graphics))

  (.beginFill graphics 0xFF3300)
  (.lineStyle graphics 4, 0xffd900, 1)
  (.drawCircle graphics 470, 90,60)


  )

(defn init2[size update_sketch]

  (def state_atom (atom {}))

  (def min-r 10)
  (def max-r 100)
  (def circle-diam 70)

  (defn setup []
    (q/fill 100)
    {:actors     [{:x       (/ (q/width) 2)
                   :y       (/ (q/height) 2)
                   :color   150
                   :name    "Actor1"
                   :pid     "<0.1.0>"
                   :pressed false}]
     :num-actors 4})

  (defn add_actor[state]
    (if (> (:num-actors state) 0)
        (do
          (println "Add actor")
;          (println state)
          (println (:num-actors state))
          (update-in (update-in state [:actors]
                   #(conj % {:x       50
                             :y       50
                             :color   150
                             :name    "Actor5"
                             :pid     "<0.1.0>"
                             :pressed false})) [:num-actors] dec))
        state
        ))

  ;; Actor state keys: :x :y :color :name :pid :pressed
  (defn draw-actor[{x :x y :y color :color name :name pid :pid}]
    (q/fill color)
    (q/ellipse x y circle-diam circle-diam))

  (defn draw [state]
    (q/background 255)
    (doseq [actor (:actors state)]
      (draw-actor actor)))

  (defn over-circle[x y diam]
    (let [disX (- x (q/mouse-x))
          disY (- y (q/mouse-y))]
      (<
        (q/sqrt (+ (q/sq disX) (q/sq disY)))
        (/ diam 2))))

  (defn mouse-pressed [state event]
    (let [new_actors_state
           (map
             (fn[actor_state]
               (if (over-circle (:x actor_state) (:y actor_state) circle-diam)
                   (assoc-in actor_state [:pressed] true)
                   actor_state))
             (:actors state))]
      (assoc-in state [:actors] new_actors_state)))

  (defn mouse-released [state event]
    (let [new_actors_state
           (map
             (fn[actor_state]
               (assoc-in actor_state [:pressed] false))
             (:actors state))]
      (assoc-in state [:actors] new_actors_state)))

  (defn mouse-dragged[state event]
    (assoc-in state [:actors]
              (map
                (fn[actor_state]
                  (if (and (:pressed actor_state)
                           (over-circle (:x actor_state) (:y actor_state) circle-diam))
                      (-> actor_state
                          (assoc-in [:x] (+ (:x actor_state) (- (q/mouse-x) (:x actor_state))))
                          (assoc-in [:y] (+ (:y actor_state) (- (q/mouse-y) (:y actor_state)))))
                      actor_state))
                (:actors state))))

  (q/defsketch actorviz
    :host "canvas-id"
    :size size
    :setup setup
    :draw draw
    :update add_actor
    :mouse-pressed mouse-pressed
    :mouse-released mouse-released
    :mouse-dragged mouse-dragged
    :middleware [m/fun-mode]))
