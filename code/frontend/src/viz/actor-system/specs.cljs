(ns viz.actor-system.specs
  (:require [cljs.spec :as s]))

; Domain model
(s/def ::pid #(re-matches #"<.*>" %))
(s/def ::from-pid ::pid)
(s/def ::to-pid ::pid)
(s/def ::type string?)
(s/def ::message string?)
(s/def ::actor (s/keys :req-un [::pid ::type]))

(defn -test []
  (assert (s/valid? ::actor {:pid "<asd>" :type "Actor2"})))