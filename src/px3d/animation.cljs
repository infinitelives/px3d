(ns px3d.animation)

; parent the Blender mesh to an empty so it can be moved around
(defn animator [gltf scene mesh-name]
  (let [container (THREE.Mesh.)
        mesh (.clone (.getObjectByName (.-scene gltf) mesh-name))
        mixer (THREE.AnimationMixer. scene)]
    (.add container mesh)
    (aset container "mixer" mixer)
    (.add scene container)
    container))

(defn stop-clip [container]
  (-> container .-mixer .stopAllAction))

(defn play-clip [container animation-name gltf scene]
  (let [clip (THREE.AnimationClip.findByName (aget gltf "animations") animation-name)]
    (stop-clip container)
    (.play (.clipAction (aget container "mixer") clip (aget container "children" 0)))))

