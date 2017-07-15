(ns viz.actor-system.specs
  (:require [cljs.spec :as s]))

; Domain model
(s/def ::pid #(re-matches #"<.*>" %))
(s/def ::from-pid ::pid)
(s/def ::to-pid ::pid)
(s/def ::type string?)
(s/def ::content string?)
(s/def ::actor (s/keys :req-un [::pid ::type]))
(s/def ::message (s/keys :req-un [::from-pid ::to-pid ::content]))

(defn -test []
  (assert (s/valid? ::actor {:pid "<asd>" :type "Actor2"}))
  (assert (s/valid? ::message {:from-pid "<1.2.1>" :to-pid "<1.2.2>" :content "{1,2}"})))