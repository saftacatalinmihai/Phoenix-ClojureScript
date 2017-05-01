(ns viz.graphics2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    cljsjs.three
    [viz.stateman :as st]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn component-reducer [[key state] action]
      (case (action :type)
            :move (if (= (action :component) key)
                    {key (action :to)}
                    {key state})
            {key state}
            ))
(defn reducer [state action]
      (into {} (map #(component-reducer % action) state)))

(defn component [x y z]
      (x #(js/console.log "Component moved x from " % " to " %2))
      (y #(js/console.log "Component moved y from " % " to " %2))
      (z #(js/console.log "Component moved z from " % " to " %2))
      )

(let [init-state {:a {:x 1 :y 1 :z 1} :b {:x 2 :y 2 :z 2}}
      store (st/create-store reducer init-state)
      a (st/bind component (st/params store [[:a :x] [:a :y] [:a :z]]))
      b (st/bind component (st/params store [[:b :x] [:b :y] [:b :z]]))]
     (st/dispatch store {:type :move :component :a :to {:x 11 :y 12 :z 13}})
     ;(st/get-state store (fn [state] (js/console.log "State1" (pr-str state))))
     (st/dispatch store {:type :move :component :b :to {:x 21 :y 2 :z 2}})
     ;(st/get-state store (fn [state] (js/console.log "State2" (pr-str state))))
     )

(def animations
  {
   :spin-x (fn [mesh] (fn [speed] (aset mesh "rotation" "x" (+ speed (.-x (.-rotation mesh))))))
   :spin-y (fn [mesh] (fn [speed] (aset mesh "rotation" "y" (+ speed (.-y (.-rotation mesh))))))
   :spin-z (fn [mesh] (fn [speed] (aset mesh "rotation" "z" (+ speed (.-z (.-rotation mesh))))))
   })

(def geometries
  {:box    #(js/THREE.BoxGeometry. %1 %2 %3)
   :sphere #(js/THREE.SphereGeometry. %1 %2 %3)})

(defn new-geometry
      [geometry]
      (fn [{scene      :scene
            camera     :camera
            renderer   :renderer
            render-fns :render-fns
            }
           {size     :size
            x        :x
            y        :y
            z        :z
            anim-fns :anim-fns}]

          (let [box ((geometries geometry) size size size)
                mat (js/THREE.MeshPhongMaterial. (js-obj "color" 0xa0a0ff))
                mesh (js/THREE.Mesh. box mat)]

               (aset mesh "position" "x" x)
               (aset mesh "position" "y" y)
               (aset mesh "position" "z" z)
               (.add scene mesh)

               (swap! render-fns concat (map #(fn [] ((((% :fn) animations) mesh) (% :speed))) anim-fns))
               mesh)))


(def new-box (new-geometry :box))
(def new-sphere (new-geometry :sphere))

(defn init [core-chan mount_elem width height]
      ;;First initiate the basic elements of a THREE scene
      (let [scene (js/THREE.Scene.)
            p-camera (js/THREE.PerspectiveCamera.
                       50 (/ width height) 1 10000)
            renderer (js/THREE.WebGLRenderer.)
            light (js/THREE.PointLight. 0xf0f0f0 0.5)
            ;controls (js/THREE.OrbitControls. p-camera)
            ]

           (.setClearColor renderer 0x505050)
           (.setPixelRatio renderer (.-devicePixelRatio js/window))
           ;;Change the starting position of cube and camera
           (aset p-camera "name" "p-camera")
           (aset p-camera "position" "z" 150)
           (.setSize renderer width height)

           ;;Add camera, mesh and box to scene and then that to DOM node.
           (.add scene p-camera)
           (.add p-camera light)
           (.appendChild mount_elem (.-domElement renderer))

           ;Kick off the animation loop updating
           (let [render-fns (atom [
                                   ;#(.update controls)
                                   ])]
                (defn animate []
                      (.requestAnimationFrame js/window animate)
                      (doseq [r @render-fns] (r))
                      (.render renderer scene p-camera))

                (animate)
                {:scene      scene
                 :camera     p-camera
                 :renderer   renderer
                 :render-fns render-fns})))