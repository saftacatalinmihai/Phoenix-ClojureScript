(ns viz.stateman
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn create-store [reducer state]
      (let [ch (chan)
            state-atom (atom state)
            watcher-id (atom 0)
            ]
           (go-loop []
                    (let [[ev-type ev-data] (<! ch)]
                         (case ev-type
                               :on-change (let [[path cb] ev-data]
                                               (add-watch state-atom (swap! watcher-id inc)
                                                          (fn [key atom old new]
                                                              (if (not= (get-in old path) (get-in new path))
                                                                (cb (get-in old path) (get-in new path)))))
                                               (cb nil (get-in @state-atom path))
                                               )
                               :dispatch (let [action ev-data]
                                              (swap! state-atom (fn [crt-state]
                                                                    ;(js/console.log "crt-state:" (pr-str crt-state))
                                                                    ;(js/console.log "action: " (pr-str action))
                                                                    (reducer crt-state action))))
                               :get-state (put! ev-data @state-atom)
                               ))
                    (recur)
                    )
           ch))

(defn on-change [store path cb] (put! store [:on-change [path cb]]))
(defn dispatch [store action] (put! store [:dispatch action]))
(defn get-state [store cb]
      (let [ch (chan)]
           (go
             (put! store [:get-state ch])
             (cb (<! ch))
             (close! ch))))

(defn params [store paths]
      (map
        #(fn [cb] (on-change store % cb))
        paths))
(defn bind [fun params]
      (apply fun params))

