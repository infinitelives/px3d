(ns px3d.engine
  (:require ["three/build/three.module.js" :as THREE]
            ["three/examples/jsm/libs/stats.module.js" :as Stats]
            ["three/examples/jsm/controls/OrbitControls.js" :as OrbitControls]
            ["three/examples/jsm/loaders/GLTFLoader.js" :as GLTFLoader]))

(defn on-window-resize [pixel-size camera renderer]
  (set! (.-aspect camera) (/ (.-innerWidth js/window) (.-innerHeight js/window)))
  (.updateProjectionMatrix camera)
  (.setSize renderer (/ (.-innerWidth js/window) pixel-size) (/ (.-innerHeight js/window) pixel-size))
  (set! (.. renderer -domElement -style -width) (str (.-innerWidth js/window) "px"))
  (set! (.. renderer -domElement -style -height) (str (.-innerHeight js/window) "px")))

(defn add-default-controls [camera renderer controls-callback]
  ; allow dragging of objects:
  ;dragControls (new THREE.DragControls #js list-of-objects camera (.-domElement renderer))
  ;(.addEventListener dragControls "dragstart" (fn [] (set! (.-enabled controls) false)))
  ;(.addEventListener dragControls "dragend" (fn [] (set! (.-enabled controls) true)))
  (let [controls (OrbitControls/OrbitControls. camera (.-domElement renderer))]
    (set! (.-rotateSpeed controls) 1.0)
    (set! (.-zoomSpeed controls) 1.2)
    (set! (.-panSpeed controls) 0.8)
    (set! (.-enableZoom controls) true)
    (set! (.-enablePan controls) false)
    (set! (.-staticMoving controls) true)
    (set! (.-enableKeys controls) false)
    (set! (.-dynamicDampingFactor controls) 0.3)
    (set! (.-maxPolarAngle controls) (- (/ (.-PI js/Math) 2) 0.175))
    (set! (.-minDistance controls) 5)
    (set! (.-maxDistance controls) 50)
    (.addEventListener controls "start" controls-callback)
    controls))

(defn init
  [& {:keys [pixel-size controls-callback] :or {pixel-size 4}}]
  (let [container (.createElement js/document "div")
        scene (THREE/Scene.)
        camera (THREE/PerspectiveCamera. 70 (/ (.-innerWidth js/window) (.-innerHeight js/window)) 1 5000)
        renderer (THREE/WebGLRenderer. #js {:antialias false})
        stats (Stats/default.)]

    (.appendChild (.-body js/document) container)

    (.set (.-position camera) 10 10 (- 10))

    (set! (.-background scene) (THREE/Color. 0xf0f0f0))
    (.setPixelRatio renderer (.-devicePixelRatio js/window))
    (.setSize
      renderer
      (/ (.-innerWidth js/window) pixel-size)
      (/ (.-innerHeight js/window) pixel-size))
    (set! (.. renderer -shadowMap -enabled) true)
    (set! (.. renderer -shadowMap -type) THREE/PCFShadowMap)
    (set! (.-gammaOutput renderer) true)
    (.appendChild container (.-domElement renderer))
    (.appendChild container (.-dom stats))
    (.addEventListener js/window "resize" (partial on-window-resize pixel-size camera renderer) false)
    (on-window-resize pixel-size camera renderer)
    {:scene scene
     :controls (add-default-controls camera renderer controls-callback)
     :camera camera
     :renderer renderer
     :stats stats}))

(defn remove-all-meshes [scene]
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      (.remove scene c))))

(defn animate [eng callback clock]
  (let [delta (.getDelta clock)
        {:keys [camera scene renderer controls stats]} @eng]
    (js/requestAnimationFrame (partial animate eng callback clock))
    (.traverse
      scene
      (fn [obj]
        (let [m (aget obj "mixer")]
          (when m
            (.update m delta)))))
    (.update controls delta)
    (when callback
      (callback delta))
    (.render renderer scene camera)
    (.update stats)
    true))

(defn start-animation-loop [eng callback]
  (let [clock (THREE/Clock.)]
    (animate eng callback clock))
  eng)

(defn add-default-lights [scene]
  (.add scene (THREE/AmbientLight. 0xffffff 1.0))

  (let [light (THREE/SpotLight. 0xffffff 1.0)]
    (.set (.-position light) 100 100 100)
    (set! (.-castShadow light) true)
    (set! (.. light -shadow -camera -near) 10)
    (set! (.. light -shadow -camera -far) 400)
    (set! (.. light -shadow -mapSize -width) 1024)
    (set! (.. light -shadow -mapSize -height) 1024)
    (set! (.-angle light) (/ (.-PI js/Math) 12))
    (.add scene light))

  (let [light (THREE/DirectionalLight. 0xffffff 0.5)]
    (.set (.-position light) 200 200 200)
    (.add scene light)))

(defn apply-shadows [assets]
  ; set up every mesh to throw and receive shadows
  (-> assets .-scene (.traverse (fn [node] (when (instance? THREE/Mesh node)
                                             (aset node "castShadow" true)
                                             (aset node "receiveShadow" true)))))
  assets)

(defn set-background [scene color]
  ; add fog
  (aset scene "fog" (THREE/FogExp2. color 0.0128 10))
  (aset scene "background" (THREE/Color. color)))

(defn setup-default-scene [scene]
  (set-background scene 0x20AAF3)
  (add-default-lights scene))

(defn load-assets [assets-url done-fn & [progress-fn error-fn]]
  (-> (GLTFLoader/GLTFLoader.)
      (.load assets-url
             #(done-fn (apply-shadows %)) progress-fn error-fn)))
