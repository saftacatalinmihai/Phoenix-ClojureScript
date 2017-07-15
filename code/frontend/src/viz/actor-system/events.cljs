(ns viz.actor-system.events
  (:require [cljs.spec :as s]
            [viz.actor-system.specs :as actor-system]))

(s/def :event/type keyword?)

(defmulti event-type :event/type)
(defmethod event-type :event/actor-spawned [_] (s/keys :req [:event/type ::actor-system/process]))
(defmethod event-type :event/message-sent [_] (s/keys :req [:event/type ::actor-system/message]))
(defmethod event-type :event/message-received [_] (s/keys :req [:event/type ::actor-system/message]))

(s/def :event/event (s/multi-spec event-type :event/type))

(defn -test []
  (assert (s/valid? :event/event {:event/type :event/actor-spawned ::actor-system/process {:pid "<some-pid>" :type "SomeActorType"}}))
  (assert (s/valid? :event/event {:event/type :event/message-sent ::actor-system/message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  (assert (s/valid? :event/event {:event/type :event/message-received ::actor-system/message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  )