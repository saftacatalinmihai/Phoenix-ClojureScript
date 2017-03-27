(ns viz.core
  (:use-macros
    [dommy.macros :only [node sel sel1]])
  (:require
    [dommy.core :as dommy]
    [quil.core :as q :include-macros true]
    [quil.middleware :as m :include-macros true]))

(.log js/console "Hey Seymore sup?!")

(def socket (new js/Phoenix.Socket
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
  (if ( == 13 (.-keyCode e))
      (do
        (.log js/console ( dommy/value (sel1 :#new-actor)))
        (.log js/console "New Actor")
        (.receive
          (channel_push "new_actor",
                        {:name ( dommy/value (sel1 :#new-actor))})
          "ok" (fn[resp] (.log js/console "Actor created ok", resp)))
      )))

(dommy/listen! (sel1 :#new-actor) :keyup new_actor!)

(def min-r 10)
(def max-r 20)

(defn setup []
  ; initial state
  {:x 0 :y 0 :r min-r, :movement :dec})

(defn draw [state]
  (q/background 255)
  (q/ellipse (:x state) (:y state) (:r state) (:r state)))

(defn update_sketch [state]
  ; increase radius of the circle by 1 on each frame
;  (.log js/console (:movement state))
  (update-in
    (cond
      ( = max-r (:r state)) (update-in state [:movement] :dec)
      ( = min-r (:r state)) (update-in state [:movement] :inc)
      :else state)
;    [:r] inc))
    [:r] (cond
       (= :dec (:movement state)) dec
       (= :inc (:movement state)) inc
           :else dec )))

; decrease radius by 1 but keeping it not less than min-r
(defn shrink [r]
  (max min-r (dec r)))

(defn mouse-moved [state event]
  (-> state
      ; set circle position to mouse position
      (assoc :x (:x event) :y (:y event))
      ; decrease radius
      (update-in [:r] shrink)))

(q/defsketch example
  :host "canvas-id"
  :size [400 400]
  :setup setup
  :draw draw
  :update update_sketch
  :mouse-moved mouse-moved
  :middleware [m/fun-mode])