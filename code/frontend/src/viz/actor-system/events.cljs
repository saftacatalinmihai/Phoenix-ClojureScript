(ns viz.actor-system.events
  (:require [cljs.spec :as s]
            [viz.actor-system.specs :as actor-system]))

(s/def ::spawn (s/keys :req-un [::actor-system/actor]))
(s/def ::message (s/keys :req-un [::actor-system/from-pid ::actor-system/to-pid ::actor-system/message]))

(defn -test []
  (assert
    (s/valid? ::spawn {:actor {:pid "<some-pid>" :type "SomeActorType"}})))