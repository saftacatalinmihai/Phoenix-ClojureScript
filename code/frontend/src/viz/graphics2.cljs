(ns viz.graphics2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    cljsjs.three
    [viz.stateman :as st]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(def init-state [
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed 0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed 0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed 0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed 0.01} {:fn :spin-y :speed -0.01} {:fn :spin-z :speed 0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed 0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed -0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed -0.01} {:fn :spin-z :speed 0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed 0.01} {:fn :spin-z :speed -0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed 0.01} {:fn :spin-y :speed -0.01} {:fn :spin-z :speed -0.01}]}
                 ;{:geometry :box :size  30 :x 0 :y 0 :z 0 :anim-fns [{:fn :spin-x :speed -0.01} {:fn :spin-y :speed -0.01} {:fn :spin-z :speed -0.01}]}
                 ])

(defn reducer [state action]
      (case (action :type)
            :move-x (update-in state [0 :x] #(+ % (action :increment)))
            :move-y (update-in state [0 :y] #(+ % (action :increment)))
            :move-z (update-in state [0 :z] #(+ % (action :increment)))
            state))
(defonce store (st/create-store reducer init-state))

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
                              mat (js/THREE.MeshPhongMaterial. (js-obj "color" 0x7777ff))
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

(defn create-earth-material []
      (let [earthTexture (js/THREE.ImageUtils.loadTexture. "imgs/earthmap4k.jpg")
            earthMaterial (js/THREE.MeshPhongMaterial. )
            ]
           (aset earthMaterial "map" earthTexture)
           earthMaterial
           ))

(defn create-cloud-material []
      (let [cloudTexture (js/THREE.ImageUtils.loadTexture. "imgs/earthcloud1k.png")
            cloudMaterial (js/THREE.MeshBasicMaterial.)
            ]
           (aset cloudMaterial "map" cloudTexture)
           (aset cloudMaterial "transparent" true)
           cloudMaterial
           ))

(defn globe [graphics]
  (let [sphereGeometry (js/THREE.SphereGeometry. 15 30 30)
        sphereMaterial (create-earth-material)
        earthMesh  (js/THREE.Mesh. sphereGeometry sphereMaterial)
        cloudGeometry (js/THREE.SphereGeometry.
                        (* 1.01 (aget sphereGeometry "parameters" "radius"))
                        (aget sphereGeometry "parameters" "widthSegments")
                        (aget sphereGeometry "parameters" "heightSegments")
                        )
        cloudMaterial (create-cloud-material)
        cloudMesh (js/THREE.Mesh. cloudGeometry cloudMaterial)
        directionalLight (js/THREE.DirectionalLight. 0xffffff 0.5)
        ambientLight (js/THREE.AmbientLight. 0x222222)
        ]
       (js/console.log (js/THREE.Vector3. 100 10 -50))
       (js/console.log "!!" directionalLight)
       (js/console.log "!!" (.-position directionalLight))
       (aset directionalLight "position" (js/THREE.Vector3. 1000 10 -50))
       (js/console.log "!!" (.-position directionalLight))
       (aset directionalLight "name" "directional")
       (aset earthMesh "name" "Earth")
       (.add (graphics :scene) directionalLight)
       (.add (graphics :scene) earthMesh)
       (.add (graphics :scene) cloudMesh)
       (.add (graphics :scene) ambientLight)
       (aset (graphics :camera) "position" "x" 35)
       (aset (graphics :camera) "position" "y" 36)
       (aset (graphics :camera) "position" "z" 33)
       (.lookAt (graphics :camera) (.-position (graphics :scene)))))

(defn init-box [graphics path store]
      (st/bind (fn [size x y z anim-fns] ((new-box graphics) size x y z anim-fns))
               (st/params store [[path :size] [path :x] [path :y] [path :z] [path :anim-fns]])))

(defn init-sphere [graphics path store]
      (st/bind (fn [size x y z anim-fns] ((new-sphere graphics) size x y z anim-fns))
               (st/params store [[path :size] [path :x] [path :y] [path :z] [path :anim-fns]])))

(defn init-from-store [graphics store]
      (st/get-state store
                    (fn [state]
                        (doall (map-indexed (fn [idx c]
                                                (case (c :geometry)
                                                      :box (init-box graphics idx store)
                                                      :sphere (init-sphere graphics idx store)))
                                            state)))))

(defn init [core-chan mount_elem width height]
      ;;First initiate the basic elements of a THREE scene
      (let [scene (js/THREE.Scene.)
            p-camera (js/THREE.PerspectiveCamera.
                       45 (/ width height) 0.1 1000)
            renderer (js/THREE.WebGLRenderer.)
            ;light (js/THREE.PointLight. 0xf0f0f0 0.5)
            controls (js/THREE.OrbitControls. p-camera)
            ]

           (.setClearColor renderer 0x000000 1.0)
           (aset renderer "shadowMapEnabled" true)
           (.setPixelRatio renderer (.-devicePixelRatio js/window))
           ;;Change the starting position of cube and camera
           (aset p-camera "name" "p-camera")
           (aset p-camera "position" "x" 15)
           (aset p-camera "position" "y" 16)
           (aset p-camera "position" "z" 13)
           (.lookAt p-camera (.-position scene))
           (.setSize renderer width height)

           ;;Add camera, mesh and box to scene and then that to DOM node.
           (.add scene p-camera)
           ;(.add p-camera light)

           (globe {:scene      scene
                   :camera     p-camera
                   :renderer   renderer})

           (.appendChild mount_elem (.-domElement renderer))

           ;Kick off the animation loop updating
           (let [render-fns (atom [
                                   #(.update controls)
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
                (globe graphics)
                graphics)))

(defn dispatch [event] (st/dispatch store event))