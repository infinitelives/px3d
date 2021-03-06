(ns px3d.core
    (:require
      ["three/build/three.module.js" :as THREE]
      [px3d.engine :as engine]
      [px3d.picker :as picker]
      [px3d.procgen :as procgen]
      [px3d.animation :as animation]
      [px3d.assets :as assets]))

(js/console.log "assets" assets/checksum)

; game state
(defonce state
  (atom {:player-target (THREE/Vector3. 8 0 8)
         :camera-lock true}))

; create the px3d engine and start an animation loop
(defonce e (engine/start-animation-loop
             (atom (engine/init :pixel-size 4 :controls-callback (fn [ev] (swap! state assoc :camera-lock false))))
             (fn [] (comment "callback inside animation loop"))))

; set up the scene with objects on reload
(let [{:keys [scene renderer controls]} @e]
  (js/console.log "scene" scene)

  (engine/remove-all-meshes scene)
  (engine/setup-default-scene scene)

  (engine/load-assets
    "models/assets.glb"
    (fn [assets]
      (js/console.log "gltf bundle:" assets)

      (procgen/seed-from-hash)

      ; This will add the entire Blender scene into the threejs scene
      ;(.add scene (.-scene assets))

      ; add a ground plane
      (let [ground (THREE/Mesh.
                     (THREE/CylinderGeometry. 150 150 1 32)
                     (THREE/MeshLambertMaterial. #js {:color 0x637C60}))]
        (aset ground "position" "y" -0.5)
        (aset ground "receiveShadow" true)
        (aset ground "castShadow" true)
        (aset ground "name" "Ground")
        (.add scene ground))

      ; create 150 randomly generated pieces of scenery

      (doseq [x (range 50)]
        (let [obj (.clone (.getObjectByName (.-scene assets) (procgen/choice ["Rock001" "Rock002" "Rock003" "Rock004" "Rock005"])))]
          (-> obj .-position (.set (* 100 (- (js/Math.random) 0.5)) 0 (* 100 (- (js/Math.random) 0.5))))
          (aset obj "rotation" "y" (* (js/Math.random) js/Math.PI 2))
          ;(aset obj "scale" "y" (+ (* (js/Math.random) 0.2) 1.0))
          (let [s (* (js/Math.random) 10.0)]
            (aset obj "scale" "x" s)
            (aset obj "scale" "y" s)
            (aset obj "scale" "z" s))
          (.add scene obj)))

      (doseq [x (range 50)]
        (let [obj (.clone (.getObjectByName (.-scene assets) (procgen/choice ["Tree001" "Tree002" "Tree003"])))]
          (-> obj .-position (.set (* 100 (- (js/Math.random) 0.5)) 0 (* 100 (- (js/Math.random) 0.5))))
          (aset obj "rotation" "y" (* (js/Math.random) js/Math.PI 2))
          (.add scene obj)))

      (doseq [x (range 20)]
        (let [obj (.clone (.getObjectByName (.-scene assets) (procgen/choice ["Mushroom001"])))]
          (-> obj .-position (.set (* 200 (- (js/Math.random) 0.5)) 0 (* 200 (- (js/Math.random) 0.5))))
          (aset obj "rotation" "y" (* (js/Math.random) js/Math.PI 2))
          (let [s (* (js/Math.random) 3.0)]
            (aset obj "scale" "x" s)
            (aset obj "scale" "y" s)
            (aset obj "scale" "z" s))
          (.add scene obj)))

      ; add a couple of meshes to animate
      (let [a (partial animation/animator assets scene)
            ship (a "Ship")
            rock (a "Rock001")
            astronaut (a "Astronaut")]
        (animation/play-clip ship "Bob" assets scene)
        (-> ship .-position (.set 10 3 10))
        (-> rock .-position (.set -5 4 -5))
        (-> rock .-scale (.set 2 3 2))
        (-> astronaut .-position (.copy (or (@state :player-last) (THREE/Vector3. 8 0 8))))
        ; center the orbit controls on the astronaut
        (aset controls "target" (.-position astronaut))

        ; add the tentacles
        (doseq [x (range 5)]
          (let [t (a "Tentacle")]
            (-> t .-position (.set  (+ -50 (* (js/Math.random) 3)) 0 (+ -30 (* (js/Math.random) 3))))
            (let [s (+ (* (js/Math.random) 0.75) 0.5)]
              (aset t "scale" "x" (* s 2))
              (aset t "scale" "y" s)
              (aset t "scale" "z" (* s 2)))
            (aset t "rotation" "y" (* (js/Math.random) js/Math.PI 2))
            (let [wave (animation/play-clip t (procgen/choice ["Wave1" "Wave2"]) assets scene)]
              (aset wave "timeScale" (+ (* (js/Math.random) 0.25) 0.1)))))

        ; remember the newly added objects
        (swap! state assoc
               :assets assets
               :astronaut astronaut
               :rock rock
               :ship ship)))))

; what to do when a mouse-pick event happens
(defn picked [objects]
  (doseq [obj objects]
    (let [{:keys [astronaut assets]} @state
          {:keys [scene]} @e]
      (js/console.log "picked:" (.-point obj) (-> obj .-object .-name))
      (animation/play-clip astronaut "Walk" assets scene)
      (swap! state assoc
             :player-target (THREE/Vector3. (-> obj .-point .-x) 0 (-> obj .-point .-z))
             :camera-lock true))))

; do some stuff in the world
; if using core.async this could be a bunch of independent
; entity loops
(defn gameloop []
  ; if the player is not on target
  (let [{:keys [astronaut rock ship player-target camera-lock]} @state
        {:keys [camera scene]} @e
        pos (if astronaut (.-position astronaut))
        quat (if astronaut (.-quaternion astronaut))]
    (when (and scene astronaut)
      (when player-target
        (if (< (.distanceTo pos player-target) 0.1)
          ; player has reached the player-target
          (do
            (swap! state assoc :player-target)
            (animation/stop-clip astronaut))
          ; player move towards player-target
          (let [move (.clone player-target)
                look (.clone player-target)
                dir (-> move (.sub pos) .normalize (.multiplyScalar 0.1))]
            (.lookAt astronaut look)
            (.rotateY astronaut (/ js/Math.PI 2.0))
            (-> astronaut .-rotation)
            (.add pos dir)
            (swap! state assoc :player-last (.clone pos)))))
      (when camera-lock
        ; gently push camera to camera follow mode
        (let [player-direction (-> (THREE/Vector3. 10 7 0) (.applyQuaternion quat))
              camera-pos (if camera (.-position camera))
              camera-move-to (-> player-direction (.add pos) (.sub camera-pos))
              camera-snap-speed 0.05
              camera-move (-> (.clone camera-move-to) (.multiplyScalar camera-snap-speed))]
          (if (> (.length camera-move-to) camera-snap-speed)
            (.add camera-pos camera-move)
            (swap! state assoc :camera-lock false))))
      ; turn the sky pink when the rock and player come close together
      (let [d (.distanceTo (.-position rock) pos)
            r (if (< d 5) 0.99 0.125)]
        (aset scene "background" "r" r)
        (aset scene "fog" "color" "r" r))
      ; float the rock around
      (let [now (* (.getTime (js/Date.)) 0.0005)]
        (-> rock .-position (.set
                              (* (js/Math.sin now) 7)
                              (+ 5 (* (js/Math.sin (* now 2.33)) 0.5))
                              (* (js/Math.cos now) 8))))))
  (js/setTimeout (partial gameloop) 20))

; handle mouse picking
(defonce picker-assigned
  (let [{:keys [camera scene renderer]} @e]
    (picker/register [scene camera renderer] #'picked)))

; kick off a singleton running the game loop
(defonce run-gameloop (gameloop))

(defn main [])

