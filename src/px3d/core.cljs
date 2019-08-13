(ns px3d.core
    (:require
      [px3d.assets :as assets]))

(def scene js/scene)
(def camera js/camera)
(def THREE js/THREE)

(def background-color 0x20AAF3)
;(def background-color 0xffffff)

(def loader (THREE.GLTFLoader.))

(defn choice [a]
  (nth a (int (* (js/Math.random) (count a)))))

(defn mouse-pick [ev raycaster container scene camera]
  (let [pos #js {:x (-> ev .-clientX (/ (aget container "clientWidth")) (* 2) (- 1))
                 :y (-> ev .-clientY (/ (aget container "clientHeight")) (* -2) (+ 1))}]
    (.setFromCamera
      raycaster
      pos
      camera)
    (.intersectObjects raycaster (.-children scene) true)))

(defn handle-pick [picked]
  (doseq [x picked]
    (js/console.log "picked:" (.-point x) (-> x .-object .-name))))

; register mouse pick event
(defonce picky
    (let [raycaster (THREE.Raycaster.)
          picker (partial mouse-pick raycaster js/container scene camera)]
      (.addEventListener js/window
                         "mousedown"
                         (fn [ev]
                           (let [picked (#'mouse-pick ev raycaster (aget js/renderer "domElement") scene camera)]
                             (#'handle-pick picked))))))

; parent the Blender mesh to an empty so it can be moved around
(defn animate [gltf scene mesh-name animation-name]
  (let [container (THREE.Mesh.)
        mesh (.clone (.getObjectByName (.-scene gltf) mesh-name))
        mixer (THREE.AnimationMixer. scene)
        clip (THREE.AnimationClip.findByName (aget gltf "animations") animation-name)]
    (.add container mesh)
    (.add scene container)
    (.play (.clipAction mixer clip mesh))
    (.push js/mixers mixer)
    container))

(defn launch [objs]
  ; clean up scene
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      ;(js/console.log "removing" c (-> c .-type (.indexOf "Light")))
      (if (== (-> c .-type (.indexOf "Light")) -1)
        (.remove scene c))))

  ; TODO: show spinner until these assets load
  (.load loader "models/assets.glb"
         (fn [gltf]
           ; set up every mesh to throw and receive shadows
           (-> gltf .-scene (.traverse (fn [node] (when (instance? THREE.Mesh node)
                                                    (aset node "castShadow" true)
                                                    (aset node "receiveShadow" true)))))

           (js/console.log "gltf bundle:" gltf)

           ; This will add the entire Blender scene into the threejs scene
           ;(.add scene (.-scene gltf))

           ; add a ground plane
           (let [ground (THREE.Mesh.
                          (THREE.CylinderGeometry. 150 150 1 32)
                          (THREE.MeshLambertMaterial. #js {:color 0x637C60 :specular 0x000000 :shininess 0}))]
             (aset ground "position" "y" -0.5)
             (aset ground "receiveShadow" true)
             (aset ground "castShadow" true)
             (aset ground "name" "Ground")
             (.add scene ground))

           ; add fog
           (aset scene "fog" (THREE.FogExp2. background-color 0.0128 10))
           (aset scene "background" (THREE.Color. background-color))

           ; create 150 randomly generated pieces of scenery
           (doseq [x (range 150)]
             (let [obj (.clone (.getObjectByName (.-scene gltf) (choice ["Tree001" "Tree002" "Tree003" "Rock001" "Rock003"])))]
               (-> obj .-position (.set (* 100 (- (js/Math.random) 0.5)) 0 (* 100 (- (js/Math.random) 0.5))))
               (aset obj "rotation" "y" (* (js/Math.random) js/Math.PI 2))
               (aset obj "scale" "y" (+ (* (js/Math.random) 0.2) 1.0))
               (.add scene obj)))

           ; add a couple of meshes to animate
           (let [animator (partial animate gltf scene)
                 ship (animator "Ship" "Bob")
                 rock (animator "Rock001" "Bob")]
             (-> ship .-position (.set 10 3 10))
             (-> rock .-position (.set -5 4 -5))
             (-> rock .-scale (.set 2 3 2))

             (aset js/controls "target" (.-position ship))

             (aset js/window "gameloop"
                   (fn [delta]
                     ; turn the sky pink when the rock and ship come close together
                     (let [d (.distanceTo (.-position rock) (.-position ship))
                           r (if (< d 3) 0.99 0.125)]
                       (aset scene "background" "r" r)
                       (aset scene "fog" "color" "r" r))
                     ; float the rock around
                     (let [now (* (.getTime (js/Date.)) 0.0005)]
                       (-> rock .-position (.set
                                             (* (js/Math.sin now) 7)
                                             4
                                             (* (js/Math.cos now) 8))))))))))

(defn mount-root []
  (console.log "re-load")
  ;(print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch nil))

(defn init! []
  (mount-root))
