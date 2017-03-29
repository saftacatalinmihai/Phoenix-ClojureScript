(ns viz.core
  (:require
    [dommy.core :as dommy :refer-macros [sel sel1]]
    [quil.core :as   q
     :include-macros true]
    [quil.middleware :as m
     :include-macros     true]))

(defn js-log [data & rest]
  (.log js/console data, rest))

(def socket
  (new js/Phoenix.Socket
       "ws://localhost:4001/socket"
       (clj->js {:params {:token :window.userToken}})))

(.connect socket)

(def channel (.channel socket "room:lobby" (clj->js {})))

(defn channel_push[msg_type msg_body]
  (.push channel msg_type (clj->js msg_body)))

(def joinedChannel (.join channel))

(def app_state (atom {}))
(defn on_ready[]
  ;; TODO: Use App state to hold all state reflected in the DOM
  ;; Then make functions that take the state and render it
  (swap! app_state assoc :actor_list [])

  (-> (js/ace.edit "editor")
      ((fn[e] (.setTheme e "ace/theme/monokai") e))
      .getSession
      (.setMode "ace/mode/elixir"))

  ;; Reset Actor List
  (dommy/set-html! (sel1 :#actor-list) "")

  (defn add_to_actor_list[actor_name]
    (dommy/append! (sel1 :#actor-list) (dommy/set-html! (dommy/create-element "LI") actor_name)))

  (.receive joinedChannel "ok"
            (fn[resp]
              (do
                (js-log "Joined successfully")
                (.receive
                  (channel_push "get_actors", {})
                  "ok" (fn [resp]
                         (let [resp_clj (js->clj resp)
                               actor_list (get resp_clj "actors")]
                           (println actor_list)
                           (doseq [actor_name actor_list]
                             (add_to_actor_list actor_name))))))))

  (.receive joinedChannel "error" (fn[resp] (js-log "Unable to join", resp)))

  (defn new_actor!
    [e]
    (if (== 13 (.-keyCode e))
        (do
          (println (dommy/value (sel1 :#new-actor)))
          (println "New Actor")
          (.receive
            (channel_push "new_actor",
                          {:name (dommy/value (sel1 :#new-actor))})
            "ok" (fn[resp]
                   (let [resp_clj (js->clj resp)]
                     (println "Actor created ok", resp_clj)
                     (println (get resp_clj "name"))
                     (println (get resp_clj "pid"))
                     (add_to_actor_list (get resp_clj "name")))
                     )))))

  (defn send_msg!
    [e]
    (if (== 13 (.-keyCode e))
        (do
;          (println (dommy/value (sel1 :#send-msg)))
          (println "Send msg")
          (.receive
            (channel_push "send_msg"
                          {:name (dommy/value (sel1 :#send-msg-actor-name))
                           :to_pid (dommy/value (sel1 :#send-msg-actor-pid)),
                           :msg (dommy/value (sel1 :#send-msg-msg))})
            "ok" (fn[resp]
                   (let [resp_clj (js->clj resp)]
                     (println "Received", resp_clj)))))))

  (defn update_actor_code!
    [e]
    (if (== 13 (.-keyCode e))
        (do
          (println "Update actor code")
          (.receive
            (channel_push "update_actor"
                          {:name (dommy/value (sel1 :#update-actor-code))
                           :actor_code ""})
            "ok" (fn[resp]
                   (let [resp_clj (js->clj resp)]
                     (println "Received", resp_clj)))
            "error" (fn[resp]
                   (let [resp_clj (js->clj resp)]
                     (println "Received error", resp_clj)))
            ))))

  (dommy/listen! (sel1 :#new-actor)     :keyup new_actor!)
  (dommy/listen! (sel1 :#send-msg-msg)  :keyup send_msg!)
  (dommy/listen! (sel1 :#update-actor-code)  :keyup update_actor_code!)

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
)

(js/jQuery (fn[] (on_ready)))
