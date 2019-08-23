(ns px3d.core
    (:require
      [px3d.engine :as engine :refer [scene camera renderer gameloop THREE]]
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
    (reset! player-target (THREE.Vector3. (-> x .-point .-x) 0 (-> x .-point .-z)))
    (js/console.log "picked:" (.-point x) (-> x .-object .-name))))

; register mouse pick event
(defonce picky
  ; TODO: convert to fn returning channel
  (let [raycaster (THREE.Raycaster.)
        picker (partial mouse-pick raycaster #js {} scene camera)
        moved (atom false)
        down (fn [ev] (reset! moved [(.-clientX ev) (.-clientY ev)]))
        done (fn [ev]
               (let [[ox oy] @moved]
               (when (and (< (js/Math.abs (- ox (.-clientX ev))) 5)
                          (< (js/Math.abs (- oy (.-clientY ev))) 5))
                 (let [picked (#'mouse-pick ev raycaster (aget renderer "domElement") scene camera)]
                   ; TODO: also return screen X,Y
                   (#'handle-pick picked)))))]
    (js/console.log "registering mouse/touch events")
    (.addEventListener js/window "mousedown" down false)
    (.addEventListener js/window "mouseup" done false)
    (.addEventListener js/window "touchstart" (fn [ev] (down (aget ev "changedTouches" 0)) (.preventDefault ev) false) false)
    (.addEventListener js/window "touchend" (fn [ev] (done (aget ev "changedTouches" 0)) (.preventDefault ev) false) false)
    true))

; parent the Blender mesh to an empty so it can be moved around
(defn animate [gltf scene mesh-name]
  (let [container (THREE.Mesh.)
        mesh (.clone (.getObjectByName (.-scene gltf) mesh-name))
        mixer (THREE.AnimationMixer. scene)]
    (.add container mesh)
    (aset container "mixer" mixer)
    (.add scene container)
    (swap! engine/mixers conj mixer)
    container))

(defn stop-clip [container]
  (-> container .-mixer .stopAllAction))

(defn play-clip [container animation-name gltf scene]
  (let [clip (THREE.AnimationClip.findByName (aget gltf "animations") animation-name)]
    (stop-clip container)
    (.play (.clipAction (aget container "mixer") clip (aget container "children" 0)))))

(defn launch [controls]
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
           (let [animator (partial animate gltf scene)
                 ship (animator "Ship")
                 rock (animator "Rock001")
                 astronaut (animator "Astronaut")]
             (play-clip ship "Bob" gltf scene)
             (-> ship .-position (.set 10 3 10))
             (-> rock .-position (.set -5 4 -5))
             (-> rock .-scale (.set 2 3 2))
             (-> astronaut .-position (.set 8 0 8))
             (js/console.log astronaut)

             (aset controls "target" (.-position astronaut))

             ;(js/setTimeout (fn []
             ;                 (-> astronaut .-mixer .stopAllAction)
             ;                 (play-clip astronaut "Walk" gltf scene)) 2000)

             ; lol what a hack
             (defonce player-animation-watcher
               (do
                 (add-watch player-target :watcher
                            (fn [key atom old-state new-state]
                              (when (and (nil? old-state) new-state)
                                (play-clip astronaut "Walk" gltf scene))))
                 true))

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
                             (stop-clip astronaut))
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
(js/console.log controls)
(defonce anim (engine/animate controls))

(defn mount-root []
  (console.log "re-load")
  ;(print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch controls))

(defn init! []
  (mount-root))

