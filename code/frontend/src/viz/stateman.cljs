(ns viz.stateman
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    cljsjs.three
    [cljs.core.async :refer [put! chan <! >! timeout close!]])
  )

(defn create-store [reducer state]
      (let [state-atom (atom state)
            watcher-id (atom 0)]
           {:on-change (fn [path cb]
                           (add-watch state-atom (swap! watcher-id inc)
                                      (fn [key atom old new]
                                          (if (not= (get-in old path) (get-in new path))
                                            (cb (get-in old path) (get-in new path))))))
            :dispatch  (fn [action]
                           (swap! state-atom (fn [crt-state]
                                                 (println "crt-state:" crt-state)
                                                 (println "action: " action)
                                                 (reducer crt-state action))))
            :get-state @state-atom}))

(defn on-change [store path cb] ((store :on-change) path cb))
(defn dispatch [store action] ((store :dispatch) action))

