(ns px3d.core
    (:require
      [reagent.core :as r]
      [px3d.assets :as assets]))

(def objects js/objects)
(def scene js/scene)
(def camera js/camera)
(def THREE js/THREE)

(def background-color 0x20AAF3)
;(def background-color 0xffffff)

(def loader (THREE.GLTFLoader.))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to Reagent"]])

;; -------------------------
;; Initialize app

(defn choice [a]
  (nth a (int (* (js/Math.random) (count a)))))

(defn make-models-hash [gltf]
  (into {} (map (fn [c] [(aget c "name") c]) (aget gltf "scene" "children"))))

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

(defn launch [objs]
  ; clean up scene
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      ;(js/console.log "removing" c (-> c .-type (.indexOf "Light")))
      (if (== (-> c .-type (.indexOf "Light")) -1)
        (.remove scene c))))

  (.load loader "models/assets.glb"
         (fn [gltf]
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
             (.add scene ground))

           ; add fog
           (aset scene "fog" (THREE.FogExp2. background-color 0.0128 10))
           (aset scene "background" (THREE.Color. background-color))

           (let [models (make-models-hash gltf)]
             (doseq [x (range 150)]
               (let [tree (.clone (get models (choice (choice [["Tree" "Tree2" "Rock001" "Rock003"]]))))]
                 (-> tree .-position (.set (* 100 (- (js/Math.random) 0.5)) 0 (* 100 (- (js/Math.random) 0.5))))
                 (aset tree "rotation" "y" (* (js/Math.random) js/Math.PI 2))
                 (aset tree "scale" "y" (+ (* (js/Math.random) 0.2) 1.0))
                 (.add scene tree)))

             ;(aset gltf "scene" "children" 2 "position" "x" 5)

             ;(.add scene (aget gltf "scene" "children" 41))

             ; parent the player object from Blender to an empty "player"
             ; Mesh so that we can position it and animate the Blender object
             (let [player (THREE.Mesh.)
                   chr (.clone (get models "Ship"))
                   mixer (THREE.AnimationMixer. scene)
                   clip (THREE.AnimationClip.findByName (aget gltf "animations") "Bob")]
               (-> player .-position (.set 10 3 10))
               (.add player chr)
               (.add scene player)
               (.play (.clipAction mixer clip chr))
               (aset js/window "mixer" mixer))))))

(defn mount-root []
  ;(r/render [home-page] (.getElementById js/document "app"))
  (console.log "re-load")
  (print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch nil))

(defn init! []
  (mount-root))
