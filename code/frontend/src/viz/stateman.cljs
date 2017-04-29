(ns viz.stateman
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]))

;(require-macros '[cljs.core.async.macros :refer [go go-loop]])
;(require '[cljs.core.async :refer [put! chan <! >! timeout close!]])


(defn create-store [reducer state]
      (let [ch (chan)
            state-atom (atom state)
            watcher-id (atom 0)
            ]
           (go-loop []
                    (let [[ev-type ev-data] (<! ch)]
                         (case ev-type)
                         :on-change (let [[path cb] ev-data]
                                         (add-watch state-atom (swap! watcher-id inc)
                                                    (fn [key atom old new]
                                                        (if (not= (get-in old path) (get-in new path))
                                                          (cb (get-in old path) (get-in new path))))))
                         :dispatch (let [action ev-data]
                                        (swap! state-atom (fn [crt-state]
                                                              (println "crt-state:" crt-state)
                                                              (println "action: " action)
                                                              (reducer crt-state action))))
                         :get-state (let [return-chan ev-data] (put! return-chan @state-atom)))
                    (recur)
                    )
           ch))

(defn on-change [store path cb] (put! store [:on-change [path cb]]))
(defn dispatch [store action] (put! store [:dispatch action]))
(defn get-state [store]
  (let [ch (chan)]
       (put! store [:get-state ch]
             (go (<! ch)))))

(defn params [store paths]
      (map
        (fn [path] (fn [cb] (on-change store path cb)))
        paths))
(defn bind [fun params]
      (apply fun params))

