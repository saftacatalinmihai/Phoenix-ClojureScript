(ns viz.graphics
  (:require
    [quil.core :as   q
     :include-macros true]
    [quil.middleware :as m
     :include-macros     true]))

(def min-r 10)

(def max-r 100)

(def circle-diam 70)

(defn setup []
  (q/fill 100)
  {:actors [{:x       (/ (q/width) 2)
             :y       (/ (q/height) 2)
             :color   150
             :name    "Actor1"
             :pid     "<0.1.0>"
             :pressed false}]})

;; Actor state keys: :x :y :color :name :pid :pressed
(defn draw-actor[{x :x y :y color :color name :name pid :pid}]
  (q/fill color)
  (q/ellipse x y circle-diam circle-diam))

(defn draw [state]
  (q/background 255)
  (doseq [actor (:actors state)]
    (draw-actor actor)))

(defn update_sketch [state]
  state)

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

(defn init[]
  (q/defsketch actorviz
    :host "canvas-id"
    :size [(-> js/window js/jQuery .width (#(* % (/ 5 10)))) 600] ;; Set the Width to the window size
    :setup setup
    :draw draw
    :update update_sketch
    :mouse-pressed mouse-pressed
    :mouse-released mouse-released
    :mouse-dragged mouse-dragged
    :middleware [m/fun-mode]))
