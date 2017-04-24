(ns viz.stateful-component
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    cljsjs.three
    [cljs.core.async :refer [put! chan <! >! timeout close!]])
  )

;; Framework

(require '[cljs.core.async :refer [put! chan <! >! timeout close!]])
(require '[clojure.core.async :refer [put! chan <! >! timeout close!]])
(require-macros '[cljs.core.async.macros :refer [go go-loop]])

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

(defn create-store [reducer init-state]
  (let [store (chan)]
    (go-loop [state init-state listeners []]
      (let [[type data] (<! store)]
        (case type
          :action (recur listeners
                         (let [old-state state
                               new-state (reducer state data)]
                              (println old-state)
                              (println new-state)
                              (println listeners)
                              (doseq [l listeners]
                                     ((l :callback)
                                       (get-in old-state (l :path))
                                       (get-in old-state (l :path))
                                       ))
                              )
                         )
          :listener (recur (conj listeners data) state)
          )))
    store))

(defn dispatch [store action] (put! store [:action action]))
(defn listener [store listener path] (put! store [:listener {:callback listener :path path}]))

(defn state-to-listeners [store state path]
      (cond
        (map? state)
          (into {} (map (fn [[k v]] {k (state-to-listeners store v (conj path k))}) state))
        (vector? state)
          (into [] (map-indexed (fn [idx v] (state-to-listeners store v (conj path idx)))) state)
        :else
          (fn [cb] (listener store cb path))))

(def s {:a 1 :b {:c 2 :d [3 4]}})

(def store (create-store (fn [store action] store) s))

(def sl (state-to-listeners store s []))

(dispatch store {:asd 1})

((get-in sl [:a]) #(println "Listener" %))



;; Testing Todo

;; Actions
(defonce next-todo-id (atom 0))

(defn add-todo [text]
      {:type :add-todo
       :id   (swap! next-todo-id inc)
       :text text})
(defn toggle-todo [id]
      {:type :toggle-todo
       :id   id})

;;Components
(defn todo [{text :text completed :completed on-click :on-click}]
      (completed (fn [c]
                     (text (fn [t]
                               (println "Todo " t " Completed " c))))))

(defn todo-list [{todos :todos on-todo-click :on-todo-click}]
      (map #(todo (into {} % {:on-click on-todo-click})) todos))

;; Reducers
(defn todo-r
      [state action]
      (case (action :type)
            :add-todo {:id        (action :id)
                       :text      (action :text)
                       :completed false
                       }
            :toggle-todo (if (not= (state :id) (action :id))
                           state
                           (assoc state :completed (not (state :completed)))
                           )
            state
            )
      )

(defn todos-r
      [state action]
      (case (action :type)
            :add-todo (conj state (todo-r nil action))
            :toggle-todo (map todo-r state)
            )
      )
