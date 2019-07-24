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

(defn launch [objs]
  ; clean up scene
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      (js/console.log "removing" c (-> c .-type (.indexOf "Light")))
      (if (== (-> c .-type (.indexOf "Light")) -1)
        (.remove scene c))))

  (.load loader "models/assets.glb"
         (fn [gltf]
           (-> gltf .-scene .-scale (.set 10 10 10))
           (-> gltf .-scene (.traverse (fn [node] (when (instance? THREE.Mesh node) (aset node "castShadow" true) (aset node "receiveShadow" true)))))
           (.add scene (.-scene gltf))
           (let [animations (aget gltf "animations")]
             (if (and animations (.-length animations))
               (let [mixer (THREE.AnimationMixer. (.-scene gltf))]
                 (doseq [a animations]
                   (js/console.log a)
                   (.play (.clipAction mixer a)))
                 (aset js/window "mixer" mixer)))))))

(defn mount-root []
  ;(r/render [home-page] (.getElementById js/document "app"))
  (console.log "re-load")
  (print "Assets checksum:" (str "0x" (.toString assets/checksum 16)))
  (launch nil))

(defn init! []
  (mount-root))
