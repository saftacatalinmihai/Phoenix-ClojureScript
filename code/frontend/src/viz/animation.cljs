(ns viz.animation)

(defn move-liniar [step-size]
  (fn [{x :x y :y from-x :from-x from-y :from-y to-x :to-x to-y :to-y}]
    (let [next-x ((if (< to-x from-x) - +) x step-size)
          proportion (/ (- next-x from-x) (- to-x from-x))
          next-y (- from-y (* proportion (- from-y to-y) ))
          ]
      (if (or
           (< (Math.abs (- to-x next-x)) step-size)
           (< (Math.abs (- to-y next-y)) step-size))
        {:x to-x :y to-y}
        {:x next-x :y next-y}
        ))))

(defn move-decelerated [speed]
  "Speed should be around 0.01 to 1. 1 means move instantly"
  (fn [{x :x y :y from-x :from-x from-y :from-y to-x :to-x to-y :to-y}]
    (let [update-f (fn [to curent] ((if (< to curent) - +) curent (* (Math.abs (- to curent)) speed)))]
      (if (and
           ( < (Math.abs (- to-x x)) 1)
           ( < (Math.abs (- to-y y)) 1))
        {:x to-x :y to-y}
        {:x (update-f to-x x)
         :y (update-f to-y y)}))))

(defn next-frame [component animation state]
  (if (not (:started animation))
    (do
      (swap! state assoc-in [:animations component :started] true)
      (set! (.-x component) (get-in animation [:from :x]))
      (set! (.-y component) (get-in animation [:from :y])))
    (if (and
         (= (get-in animation [:to :x]) (.-x component))
         (= (get-in animation [:to :y]) (.-y component)))
      (swap! state update-in [:animations] (fn [anims] (dissoc anims component)))
      (let [{next-x :x next-y :y} ((:anim-function animation) {:x (.-x component)
                                                               :y (.-y component)
                                                               :to-x (get-in animation [:to :x])
                                                               :to-y (get-in animation [:to :y])
                                                               :from-x (get-in animation [:from :x])
                                                               :from-y (get-in animation [:from :y])
                                                               })]
        (set! (.-x component) next-x)
        (set! (.-y component) next-y)))))
