(ns viz.stateful-component
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    cljsjs.three
    [cljs.core.async :refer [put! chan <! >! timeout close!]])
  )

(require '[cljs.core.async :refer [put! chan <! >! timeout close!]])
(require '[clojure.core.async :refer [put! chan <! >! timeout close!]])

(defn component
      [{
        id    :id
        comp  :component
        props :props
        state :state
        ch    :event-channel
        }]
      (let [prop-fns
            (into {}
                  (map
                    (fn [[prop-name prop-value]]
                        (let [prop-state (atom prop-value)
                              watchers (atom [])]
                             (swap! state assoc-in [id prop-name] prop-state)
                             (add-watch prop-state [id prop-name]
                                        (fn [key atom old-state new-state]
                                            (doseq [w @watchers]
                                                   (if (not= old-state new-state)
                                                     (w old-state new-state))
                                                   )))
                             {prop-name (fn [callback]
                                            (callback nil @prop-state)
                                            (swap! watchers conj callback))}))
                    props))]
           (comp {:event-channel ch :props (into {} prop-fns)})))


(defn mock-comp [{ch     :event-channel
                  {x :x} :props
                  }]
      (x #(println "old " %1 " new " %2))
      )
(defn mock-comp2 [{ch :event-channel
                   {x :x y :y} :props}]
      (x #(println "new-x" %2))
      (y #(println "new-y" %2))
      )

(def mock-state (atom {}))
(component {:id 1 :component mock-comp :props {:x 1} :state mock-state :event-channel (chan)})
(component {:id 2 :component mock-comp2 :props {:x 1 :y 2} :state mock-state :event-channel (chan)})
(reset! (get-in @mock-state [1 :x]) 4)
(reset! (get-in @mock-state [2 :y]) 4)
