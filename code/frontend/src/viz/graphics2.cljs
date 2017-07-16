(ns viz.graphics2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    cljsjs.three
    [viz.stateman :as st]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed -0.01}]}
(def init-state {
                 "asd" {:geometry :box :size  10 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed -0.01}]}})

(defn reducer [state action]
      (case (action :type)
            :new-sphere (assoc-in state [(action :id)] action)
            :move-x (update-in state [0 :x] #(+ % (action :increment)))
            :move-y (update-in state [0 :y] #(+ % (action :increment)))
            :move-z (update-in state [0 :z] #(+ % (action :increment)))
            state))

;(js/console.log "Store0: " (pr-str init-state))
(defonce store (st/create-store reducer init-state))
;(st/get-state store #(js/console.log "Store1: " (pr-str %1)))

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
            }]
          (fn [size x y z anim-fns]
              (size (fn [old new-size]
                        (let [box ((geometries geometry) new-size new-size new-size)
                              mat (js/THREE.MeshNormalMaterial.)
                              mesh (js/THREE.Mesh. box mat)]

                             (x #(aset mesh "position" "x" %2))
                             (y #(aset mesh "position" "y" %2))
                             (z #(aset mesh "position" "z" %2))
                             (.add scene mesh)
                             (anim-fns (fn [o new-anims]
                                           (swap! render-fns concat (map #(fn [] ((((% :fn) animations) mesh) (% :speed))) new-anims)))
                                       )))))))

(def new-box (new-geometry :box))
(def new-sphere (new-geometry :sphere))

(defn init-box [graphics path store]
      (st/bind (fn [size x y z anim-fns] ((new-box graphics) size x y z anim-fns))
               (st/params store [[path :size] [path :x] [path :y] [path :z] [path :anim-fns]])))

(defn init-sphere [graphics path store]
      (st/bind (fn [size x y z anim-fns] ((new-sphere graphics) size x y z anim-fns))
               (st/params store [[path :size] [path :x] [path :y] [path :z] [path :anim-fns]])))

(defn init-from-store [graphics store]
      (st/get-state store
                    (fn [state]
                      ;(js/console.log (pr-str "State:" state))
                        (doall (map (fn [[id c]]
                                      (do
                                        ;(js/console.log (pr-str id))
                                        ;(js/console.log (pr-str c))
                                          (case (c :geometry)
                                            :box (init-box graphics id store)
                                            :sphere (init-sphere graphics id store)
                                            (js/console.log "Unhandled geometry:" (pr-str c)))))
                                            state)))))

(defn init [core-chan mount_elem width height]
      ;;First initiate the basic elements of a THREE scene
      (let [scene (js/THREE.Scene.)
            p-camera (js/THREE.PerspectiveCamera.
                       45 (/ width height) 0.1 1000)
            renderer (js/THREE.WebGLRenderer.)
            ;light (js/THREE.PointLight. 0xf0f0f0 0.5)
            ;controls (js/THREE.OrbitControls. p-camera)
            ]

           (.setClearColor renderer 0xa0a0a0 1.0)
           (aset renderer "shadowMapEnabled" true)
           (.setPixelRatio renderer (.-devicePixelRatio js/window))
           ;;Change the starting position of cube and camera
           (aset p-camera "name" "p-camera")
           (aset p-camera "position" "x" 25)
           (aset p-camera "position" "y" 26)
           (aset p-camera "position" "z" 23)
           (.lookAt p-camera (.-position scene))
           (.setSize renderer width height)

           ;;Add camera, mesh and box to scene and then that to DOM node.
           (.add scene p-camera)

           (.appendChild mount_elem (.-domElement renderer))

           ;Kick off the animation loop updating
           (let [render-fns (atom [
                                   ;#(.update controls)
                                   ])
                 graphics {:scene      scene
                           :camera     p-camera
                           :renderer   renderer
                           :render-fns render-fns}]
                (defn animate []
                      (.requestAnimationFrame js/window animate)
                      (doseq [r @render-fns] (r))
                      (.render renderer scene p-camera))

                (animate)
                (init-from-store graphics store)
                graphics)))

(defn dispatch [event] (st/dispatch store event))