(ns viz.view.state3d
  (:require [cljs.spec :as s]
            [viz.actor-system.events :as ev]
            [viz.actor-system.specs :as actor-system]))

;; Specs:
(s/def ::direction int?)
(s/def ::point (s/cat ::x ::direction ::y ::direction ::z ::direction))
(s/def ::position ::point)

(s/fdef next-point
        :args (s/coll-of ::point)
        :ret ::point)

;; Impl
(defn next-point
  [existing-points]
  (if (empty? existing-points)
    [0 0 0]
    (update-in (apply max 1 existing-points) [1] #(+ 10 %))))

(defn get-running-actors-pos [state]
  (->> (get-in state [:running-actors])
       (vals)
       (filter #(contains? % ::position))
       (map ::position)))

;; The State keeper implementation of EventHandler
(defmulti handle-event :event/type)
(defmethod handle-event :event/actor-spawned [ev state]
  (update-in state
             [:running-actors (get-in ev [::actor-system/actor :pid])]
             assoc
             ::position (next-point (get-running-actors-pos state))))
(defmethod handle-event :event/message-sent [ev state] state)
(defmethod handle-event :event/message-received [ev state] state)

(deftype State [state-atom]
  ev/EventHandler
  (handle [this ev] (swap! state-atom #(handle-event ev %))))

(defn state [state-atom] (State. state-atom))

(defn -test []
  (js/console.log (pr-str (s/conform ::point [1 2 3]))))