(ns viz.stateman-test
  (:require [viz.stateman :refer ]))

;; Test
(def store (create-store (fn [s a] {:name (a :payload)}) {:name "Mihai"}))
(defn name-printer [store]
      ((store :on-change) [:name] #(println "old: " %1 " new " %2))
      ((store :dispatch) {:type :ok :payload "AASD"})
      )
(name-printer store)

(defn r [state action]
  (case (action :type)
        :inc (inc state)
        :dec (dec state)))

(defn counter [store dispatch]
  (on-change store [] #(println "old: " %1 " new " %2))
  (dispatch {:type :inc}))

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
            :toggle-todo (map #(todo-r % action) state)
            )
      )
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

(def todos [{:id 1 :text "ASD" :completed false} {:id 2 :text "qwe" :completed true}])
(def store (create-store todos-r todos))
(on-change store [] #(println "Changed" % %2))
(dispatch store (add-todo "ASD"))
(dispatch store (add-todo "ASD3"))
(dispatch store (toggle-todo 1))