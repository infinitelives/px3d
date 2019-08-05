(ns px3d.core
    (:require
      [reagent.core :as r]
      [px3d.assets :as assets]))

(def objects js/objects)
(def scene js/scene)
(def THREE js/THREE)

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

(defn launch [objs]
  ; clean up scene
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      (js/console.log "removing" c (-> c .-type (.indexOf "Light")))
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

           (doseq [x (range 10)]
             (let [tree (.clone (aget gltf "scene" "children" 40))]
               (-> tree .-position (.set (* 20 (- (js/Math.random) 0.5)) 0 (* 20 (- (js/Math.random) 0.5))))
               (.add scene tree)))

           ;(aset gltf "scene" "children" 2 "position" "x" 5)

           (.add scene (aget gltf "scene" "children" 41))

           ; parent the player object from Blender to an empty "player"
           ; Mesh so that we can position it and animate the Blender object
           (let [player (THREE.Mesh.)
                 chr (.clone (aget gltf "scene" "children" 2))
                 mixer (THREE.AnimationMixer. scene)
                 clip (THREE.AnimationClip.findByName (aget gltf "animations") "Bob")]
             (-> player .-position (.set 10 0 10))
             (.add player chr)
             (.add scene player)
             (.play (.clipAction mixer clip chr))
             (aset js/window "mixer" mixer)))))

(defn mount-root []
  ;(r/render [home-page] (.getElementById js/document "app"))
  (console.log "re-load")
  (print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch nil))

(defn init! []
  (mount-root))
