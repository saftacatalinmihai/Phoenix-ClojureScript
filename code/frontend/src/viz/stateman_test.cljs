(ns viz.stateman2)

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

(defn counter [store]
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
(let [store (create-store todos-r todos)]
     (on-change store [] #(println "Changed" % %2))

     (bind
       (fn [todos] (todos (fn [old new] (println "Old: " old "new: " new))))
       (params store [[0]])
       )

     (dispatch store (add-todo "ASD"))
     (dispatch store (add-todo "ASD3"))
     (dispatch store (toggle-todo 1))
     (dispatch store (toggle-todo 1))
     (dispatch store (toggle-todo 2))
     )

(defn component-reducer [[key state] action]
      (println key state action)
      (case (action :type)
            :move (if (= (action :component) key)
                    {key (action :to)}
                    {key state})
            {key state}
            ))
(defn reducer [state action]
      (into {} (map #(component-reducer % action) state)))

(defn component [x y z]
      (x #(println "Component moved x from " % " to " %2))
      (y #(println "Component moved y from " % " to " %2))
      (z #(println "Component moved z from " % " to " %2))
      )
(def init-state {:a {:x 1 :y 1 :z 1} :b {:x 2 :y 2 :z 2}})
(def store (create-store reducer init-state))
(def a (bind component (params store [[:a :x] [:a :y] [:a :z]])))

(let [init-state {:a {:x 1 :y 1 :z 1} :b {:x 2 :y 2 :z 2}}
      store (create-store reducer init-state)
      a (bind component (params store [[:a :x] [:a :y] [:a :z]]))
      b (bind component (params store [[:b :x] [:b :y] [:b :z]]))]
     (dispatch store {:type :move :component :a :to {:x 11 :y 12 :z 13}})
     (println "Store:" (store :get-state))
     (dispatch store {:type :move :component :b :to {:x 21 :y 2 :z 2}})
     (println "Store:" (store :get-state))
     )
