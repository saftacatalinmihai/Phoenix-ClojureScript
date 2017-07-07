(ns viz.globe)

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
