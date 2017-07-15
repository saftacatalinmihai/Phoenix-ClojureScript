(ns viz.actor-system.events
  (:require [cljs.spec :as s]
            [viz.actor-system.specs :as actor-system]))

(s/def ::actor-spawned (s/keys :req-un [::actor-system/actor]))
(s/def ::message-sent (s/keys :req-un [::actor-system/message]))
(s/def ::message-received (s/keys :req-un [::actor-system/message]))

(defn -test []
  (assert (s/valid? ::actor-spawned {:actor {:pid "<some-pid>" :type "SomeActorType"}}))
  (assert (s/valid? ::message-sent {:message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  (assert (s/valid? ::message-received {:message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"}}))
  )