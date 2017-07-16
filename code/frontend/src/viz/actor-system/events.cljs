(ns viz.actor-system.events
  (:require [cljs.spec :as s]
            [viz.actor-system.specs :as actor-system]))

(s/def :event/type keyword?)

(defmulti event-type :event/type)
(defmethod event-type :event/actor-spawned [_] (s/keys :req [:event/type ::actor-system/actor]))
(defmethod event-type :event/actor-stopped [_] (s/keys :req [:event/type ::actor-system/actor]))
(defmethod event-type :event/message-sent [_] (s/keys :req [:event/type ::actor-system/message]))
(defmethod event-type :event/message-received [_] (s/keys :req [:event/type ::actor-system/message]))

(s/def :event/event (s/multi-spec event-type :event/type))


;; The Event Handler protocol
(defprotocol EventHandler
  "Handles an actor system event"
  (handle [this ev] "Handle the event"))


;; The event store implementation for EventHandler
(deftype Store [event-list]
  EventHandler
  (handle [this ev] (swap! event-list conj ev)))

(defn store
  ([] (Store. (atom [])))
  ([ev-list] (Store. (atom ev-list))))

(defn get-events [store] @(.-event-list store))

(defonce sticky-store (store))

(defn -test []
  (let [e {:event/type :event/actor-spawned ::actor-system/actor {:pid "<1.2.1>" :type "SomeActorType"}}
        e2 {:event/type :event/actor-spawned ::actor-system/actor {:pid "<1.2.2>" :type "SomeActorType2"}}
        ]
    (js/console.log (pr-str (handle sticky-store e)))
    (js/console.log (pr-str (handle sticky-store e2)))
    (js/console.log (pr-str (handle sticky-store e)))
    (js/console.log (pr-str (get-events sticky-store)))
    )

  (assert (s/valid? :event/event {:event/type :event/actor-spawned ::actor-system/actor {:pid "<some-pid>" :type "SomeActorType"}}))
  (assert (s/valid? :event/event {:event/type :event/actor-stopped ::actor-system/actor {:pid "<some-pid>" :type "SomeActorType"}}))
  (assert (s/valid? :event/event {:event/type :event/message-sent ::actor-system/message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  (assert (s/valid? :event/event {:event/type :event/message-received ::actor-system/message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  )
