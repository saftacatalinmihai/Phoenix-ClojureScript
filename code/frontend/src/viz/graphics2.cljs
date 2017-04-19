(ns viz.graphics2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   cljsjs.three
   [cljs.core.async :refer [put! chan <! >! timeout close!]]
   ))

(def animations
  {
   :spin-x (fn [mesh] (fn [speed] (aset mesh "rotation" "x" (+ speed (.-x (.-rotation mesh))))))
   :spin-y (fn [mesh] (fn [speed] (aset mesh "rotation" "y" (+ speed (.-y (.-rotation mesh))))))
   :spin-z (fn [mesh] (fn [speed] (aset mesh "rotation" "z" (+ speed (.-z (.-rotation mesh))))))
})

(def geometries
  {:box #(js/THREE.BoxGeometry. %1 %2 %3)
   :sphere #(js/THREE.SphereGeometry. %1 %2 %3)})

(defn new-geometry
  [geometry]
  (fn [{scene :scene
         camera :camera
         renderer :renderer
         render-fns :render-fns
         }
        {size :size
         x :x
         y :y
         z :z
         anim-fns :anim-fns}
        ]
    (let [box  ((geometries geometry) size size size)
          mat  (js/THREE.MeshPhongMaterial. (js-obj "color" 0xa0a0ff))
          mesh (js/THREE.Mesh. box mat)
          ]
      (aset mesh "position" "x" x)
      (aset mesh "position" "y" y)
      (aset mesh "position" "z" z)
      (.add scene mesh)

      (swap! render-fns concat (map #( fn [] ((((% :fn) animations) mesh) (% :speed))) anim-fns))
      mesh
      ))
  )

(def new-box (new-geometry :box))
(def new-sphere (new-geometry :sphere))

(defn init [core-chan mount_elem width height]
  ;;First initiate the basic elements of a THREE scene
  (let [scene    (js/THREE.Scene.)
        p-camera (js/THREE.PerspectiveCamera.
                    50 (/ width height) 1 10000)
        renderer (js/THREE.WebGLRenderer.)
        light    (js/THREE.PointLight. 0xf0f0f0 0.5)
        controls (js/THREE.OrbitControls. p-camera)
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
    (let [render-fns (atom [#(.update controls)])]
      (defn animate []
        (.requestAnimationFrame js/window animate)
        (doseq [r @render-fns] (r))
        (.render renderer scene p-camera))

      (animate)
      (new-box
       {:scene scene
        :camera p-camera
        :renderer renderer
        :render-fns render-fns}
       {:size 30 :x 0 :y 0 :z 0
        :anim-fns [{:fn :spin-x :speed 0.01}
               {:fn :spin-y :speed 0.01}]})

      {:scene scene
       :camera p-camera
       :renderer renderer
       :render-fns render-fns
       }
      )))
