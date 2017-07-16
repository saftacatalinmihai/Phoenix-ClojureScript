(ns viz.actor-system.state
  (:require [viz.actor-system.events :as ev]
            [viz.actor-system.specs :as actor-system]))

;; The State keeper implementation of EventHandler
(defmulti handle-event :event/type)

(defmethod handle-event :event/actor-spawned [ev state]
  (assoc-in state [:running-actors (get-in ev [::actor-system/process :pid])] (::actor-system/process ev)))

(defmethod handle-event :event/message-sent [ev state] state)
(defmethod handle-event :event/message-received [ev state] state)

(deftype State [state-atom]
  ev/EventHandler
  (handle [this ev] (swap! state-atom #(handle-event ev %))))

(defonce sticky-state (State. (atom {})))

(defn -test []
  (let [e {:event/type :event/actor-spawned ::actor-system/process {:pid "<1.2.1>" :type "SomeActorType"}}
        e2 {:event/type :event/actor-spawned ::actor-system/process {:pid "<1.2.2>" :type "SomeActorType2"}}
        ]
    (js/console.log (pr-str "State: " (ev/handle sticky-state e)))
    (js/console.log (pr-str "State: " (ev/handle sticky-state e2)))
    ))
