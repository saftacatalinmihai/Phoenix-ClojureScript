(ns viz.actor-system.events
  (:require [cljs.spec :as s]
            [viz.actor-system.specs :as actor-system]))

(s/def ::spawn (s/keys :req [::actor-system/actor]))
(s/def ::message (s/keys :req [::actor-system/from-pid ::actor-system/to-pid ::actor-system/message]))

(defn -test []
  (assert
    (s/valid? ::spawn {::actor-system/actor {::actor-system/pid "<some-pid>" ::actor-system/type "SomeActorType"}})))