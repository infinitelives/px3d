(ns px3d.core
    (:require
      [px3d.engine :as engine :refer [scene camera renderer gameloop THREE]]
      [px3d.picker :as picker]
      [px3d.animation :as animation]
      [px3d.assets :as assets]))

(def background-color 0x20AAF3)
;(def background-color 0xffffff)

(def loader (THREE.GLTFLoader.))

(defonce player-target (atom nil))

; seed random number generator from URL hash fragment
(let [hashfrag (-> js/window .-location .-hash (.substr 1))
      hashfrag (if (= hashfrag "") (-> js/Math .random .toString (.split ".") .pop) hashfrag)]
  (aset js/window "location" "hash" hashfrag)
  (.seedrandom js/Math hashfrag))

(defn choice [a]
  (nth a (int (* (js/Math.random) (count a)))))

(defn launch [controls]
  (js/console.log "scene" engine/scene)

  ; clean up scene
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      (.remove scene c)))

  (engine/add-default-lights scene)

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
                          (THREE.MeshLambertMaterial. #js {:color 0x637C60}))]
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
           (let [a (partial animation/animator gltf scene)
                 ship (a "Ship")
                 rock (a "Rock001")
                 astronaut (a "Astronaut")]
             (animation/play-clip ship "Bob" gltf scene)
             (-> ship .-position (.set 10 3 10))
             (-> rock .-position (.set -5 4 -5))
             (-> rock .-scale (.set 2 3 2))
             (-> astronaut .-position (.set 8 0 8))
             (js/console.log astronaut)

             (aset controls "target" (.-position astronaut))

             (defn picked [objects]
               (doseq [x objects]
                 (reset! player-target (THREE.Vector3. (-> x .-point .-x) 0 (-> x .-point .-z)))
                 (animation/play-clip astronaut "Walk" gltf scene)
                 (js/console.log "picked:" (.-point x) (-> x .-object .-name))))

             ; handle mouse picking
             (defonce pick
               (picker/register [scene camera renderer] #'picked))

             (reset! gameloop
                     (fn [delta]
                       ; if the player is not on target
                       (let [target @player-target
                             pos (.-position astronaut)]
                         (when target
                           (if (< (.distanceTo pos target) 0.1)
                             ; player has reached target
                             (do
                               (reset! player-target nil)
                               (animation/stop-clip astronaut))
                             ; player move towards target
                             (let [move (.clone target)
                                   look (.clone target)
                                   dir (-> move (.sub pos) .normalize (.multiplyScalar 0.1))]
                               (.lookAt astronaut look)
                               (.rotateY astronaut (/ Math.PI 2.0))
                               (-> astronaut .-rotation)
                               (.add pos dir)))))
                       ; turn the sky pink when the rock and player come close together
                       (let [d (.distanceTo (.-position rock) (.-position astronaut))
                             r (if (< d 5) 0.99 0.125)]
                         (aset scene "background" "r" r)
                         (aset scene "fog" "color" "r" r))
                       ; float the rock around
                       (let [now (* (.getTime (js/Date.)) 0.0005)]
                         (-> rock .-position (.set
                                               (* (js/Math.sin now) 7)
                                               (+ 5 (* (js/Math.sin (* now 2.33)) 0.5))
                                               (* (js/Math.cos now) 8))))))))))

(defonce e (engine/init :pixel-size 4))
(defonce controls (engine/add-default-controls engine/camera engine/renderer))
(defonce anim (engine/animate controls scene))

(defn mount-root []
  (console.log "re-load")
  ;(print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch controls))

(defn init! []
  (mount-root))

