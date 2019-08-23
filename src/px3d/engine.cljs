(ns px3d.engine)

(defonce gameloop (atom nil))

(defonce THREE js/THREE)

(defonce renderer (new THREE.WebGLRenderer #js {:antialias false}))
(defonce stats (new js/Stats))
(defonce clock (new THREE.Clock))

(def loader (THREE.GLTFLoader.))

(defn on-window-resize [pixel-size camera]
  (set! (.-aspect camera) (/ (.-innerWidth js/window) (.-innerHeight js/window)))
  (.updateProjectionMatrix camera)
  (.setSize renderer (/ (.-innerWidth js/window) pixel-size) (/ (.-innerHeight js/window) pixel-size))
  (set! (.. renderer -domElement -style -width) (str (.-innerWidth js/window) "px"))
  (set! (.. renderer -domElement -style -height) (str (.-innerHeight js/window) "px")))

(defn add-default-controls [camera renderer]
  ; allow dragging of objects:
  ;dragControls (new THREE.DragControls #js list-of-objects camera (.-domElement renderer))
  ;(.addEventListener dragControls "dragstart" (fn [] (set! (.-enabled controls) false)))
  ;(.addEventListener dragControls "dragend" (fn [] (set! (.-enabled controls) true)))
  (let [controls (THREE.OrbitControls. camera (.-domElement renderer))]
    (set! (.-rotateSpeed controls) 1.0)
    (set! (.-zoomSpeed controls) 1.2)
    (set! (.-panSpeed controls) 0.8)
    (set! (.-enableZoom controls) true)
    (set! (.-enablePan controls) true)
    (set! (.-staticMoving controls) true)
    (set! (.-dynamicDampingFactor controls) 0.3)
    (set! (.-maxPolarAngle controls) (- (/ (.-PI js/Math) 2) 0.175))
    (set! (.-minDistance controls) 5)
    (set! (.-maxDistance controls) 50)
    controls))

(defn init
  [& {:keys [pixel-size] :or {pixel-size 4}}]
  (let [container (.createElement js/document "div")
        scene (THREE.Scene.)
        camera (THREE.PerspectiveCamera. 70 (/ (.-innerWidth js/window) (.-innerHeight js/window)) 1 5000)]

    (.appendChild (.-body js/document) container)

    (.set (.-position camera) 10 10 (- 10))

    (set! (.-background scene) (new THREE.Color 0xf0f0f0))
    (.setPixelRatio renderer (.-devicePixelRatio js/window))
    (.setSize
      renderer
      (/ (.-innerWidth js/window) pixel-size)
      (/ (.-innerHeight js/window) pixel-size))
    (set! (.. renderer -shadowMap -enabled) true)
    (set! (.. renderer -shadowMap -type) (.-PCFShadowMap THREE))
    (set! (.-gammaOutput renderer) true)
    (.appendChild container (.-domElement renderer))
    (.appendChild container (.-dom stats))
    (.addEventListener js/window "resize" (partial on-window-resize pixel-size camera) false)
    (on-window-resize pixel-size camera)
    {:scene scene :controls (add-default-controls camera renderer) :camera camera}))

(defn remove-all-meshes [scene]
  (let [children (-> scene .-children .slice)]
    (doseq [c children]
      (.remove scene c))))

(defn animate [controls scene camera]
  (let [delta (.getDelta clock)]
    (js/requestAnimationFrame (partial animate controls scene camera))
    (.traverse
      scene
      (fn [obj]
        (let [m (aget obj "mixer")]
          (when m
            (.update m delta)))))
    (.update controls delta)
    (when-let [f @gameloop] (f delta))
    (.render renderer scene camera)
    (.update stats)
    true))

(defn add-default-lights [scene]
  (.add scene (new THREE.AmbientLight 0xffffff 1.0))

  (let [light (new THREE.SpotLight 0xffffff 1.0)]
    (.set (.-position light) 100 100 100)
    (set! (.-castShadow light) true)
    (set! (.. light -shadow -camera -near) 10)
    (set! (.. light -shadow -camera -far) 400)
    (set! (.. light -shadow -mapSize -width) 1024)
    (set! (.. light -shadow -mapSize -height) 1024)
    (set! (.-angle light) (/ (.-PI js/Math) 12))
    (.add scene light))

  (let [light (new THREE.DirectionalLight 0xffffff 0.5)]
    (.set (.-position light) 200 200 200)
    (.add scene light)))

(defn apply-shadows [assets]
  ; set up every mesh to throw and receive shadows
  (-> assets .-scene (.traverse (fn [node] (when (instance? THREE.Mesh node)
                                             (aset node "castShadow" true)
                                             (aset node "receiveShadow" true)))))
  assets)

(defn set-background [scene color]
  ; add fog
  (aset scene "fog" (THREE.FogExp2. color 0.0128 10))
  (aset scene "background" (THREE.Color. color)))

(defn setup-default-scene [scene]
  (set-background scene 0x20AAF3)
  (add-default-lights scene))

(defn load-assets [assets-url done-fn & [progress-fn error-fn]]
  (.load loader assets-url #(done-fn (apply-shadows %)) progress-fn error-fn))
