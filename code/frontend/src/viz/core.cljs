(ns viz.core
  (:use-macros
    [dommy.macros :only [node sel sel1]])
  (:require
    [dommy.core :as dommy]
    [quil.core :as   q
     :include-macros true]
    [quil.middleware :as m
     :include-macros     true]))

(.log js/console "Hey Seymore sup?!")

(def socket
  (new js/Phoenix.Socket
       "ws://localhost:4001/socket"
       (clj->js {:params {:token :window.userToken}})))

(.connect socket)

(def channel (.channel socket "room:lobby" (clj->js {})))

(def joinedChannel (.join channel))

(.receive joinedChannel "ok" (fn[resp] (.log js/console "Joined successfully", resp)))
(.receive joinedChannel "error" (fn[resp] (.log js/console "Unable to join", resp)))

(defn channel_push[msg_type msg_body]
  (.push channel msg_type (clj->js msg_body)))

(defn new_actor!
  [e]
  (if (== 13 (.-keyCode e))
      (do
        (.log js/console (dommy/value (sel1 :#new-actor)))
        (.log js/console "New Actor")
        (.receive
          (channel_push "new_actor",
                        {:name (dommy/value (sel1 :#new-actor))})
          "ok" (fn[resp] (.log js/console "Actor created ok", resp))))))


(dommy/listen! (sel1 :#new-actor) :keyup new_actor!)

;;===================;;
;; Drawing functions ;;
;;===================;;

(def min-r 10)
(def max-r 100)
(def circle-diam 70)

;(defn new-actor-q [name]
;  {:x (/ (q/width) 2) :y (/ (q/height) 2)})

(defn setup []
  (q/fill 100)
  {:x (/ (q/width) 2) :y (/ (q/height) 2) :pressed false})

;  {:actors [(new-actor-q "Actor1")]})

(defn draw [state]
  (q/background 255)
  ;  (doseq [actor (:actors state)]
  (q/ellipse (:x state) (:y state) circle-diam circle-diam))

;)

(defn update_sketch [state]
  state )
  ; increase radius of the circle by 1 on each frame
;  (update-in
;    (cond (= max-r (:r state)) (assoc-in state [:movement] dec)
;          (= min-r (:r state)) (assoc-in state [:movement] inc)
;          :else                state)
;    [:r] (:movement state)))

; decrease radius by 1 but keeping it not less than min-r
;(defn shrink [r]
;  (max min-r (dec r)))

(defn over-circle[x y diam]
  (let [disX (- x (q/mouse-x))
        disY (- y (q/mouse-y))]
    (<
      (q/sqrt (+ (q/sq disX) (q/sq disY)))
      (/ diam 2))))

(defn mouse-pressed [state event]
  (if (over-circle (:x state) (:y state) circle-diam)
      (assoc-in state [:pressed] true)
      state))

(defn mouse-released [state event]
    (assoc-in state [:pressed] false))

(defn mouse-dragged[state event]
  (if (and (:pressed state) (over-circle (:x state) (:y state) circle-diam))
      (-> state
          (assoc-in [:x] ( + (:x state) (- (q/mouse-x) (q/pmouse-x))))
          (assoc-in [:y] ( + (:y state) (- (q/mouse-y) (q/pmouse-y)))))
      state ))

(q/defsketch example
  :host "canvas-id"
  :size [400 400]
  :setup setup
  :draw draw
  :update update_sketch
  :mouse-pressed mouse-pressed
  :mouse-released mouse-released
  :mouse-dragged mouse-dragged
  :middleware [m/fun-mode])