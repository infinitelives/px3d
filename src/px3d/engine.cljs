(ns px3d.engine)

(defonce engine (atom {:started? false}))
(def mixers (atom []))
(defonce gameloop (atom nil))

(defonce THREE js/THREE)

(defonce scene (new THREE.Scene))
(defonce camera (new THREE.PerspectiveCamera 70 (/ (.-innerWidth js/window) (.-innerHeight js/window)) 1 5000))
  
(defonce renderer (new THREE.WebGLRenderer #js {:antialias false}))
(defonce controls (new THREE.OrbitControls camera (.-domElement renderer)))
(defonce stats (new js/Stats))
(defonce clock (new THREE.Clock))

(defn on-window-resize [pixel-size]
  (set! (.-aspect camera) (/ (.-innerWidth js/window) (.-innerHeight js/window)))
  (.updateProjectionMatrix camera)
  (.setSize renderer (/ (.-innerWidth js/window) pixel-size) (/ (.-innerHeight js/window) pixel-size))
  (set! (.. renderer -domElement -style -width) (str (.-innerWidth js/window) "px"))
  (set! (.. renderer -domElement -style -height) (str (.-innerHeight js/window) "px")))

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

(defn init
  [& {:keys [pixel-size] :or {pixel-size 4}}]
  (let [;dragControls (new THREE.DragControls #js [] camera (.-domElement renderer))
        container (.createElement js/document "div")]

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

    ;(.addEventListener dragControls "dragstart" (fn [] (set! (.-enabled controls) false)))
    ;(.addEventListener dragControls "dragend" (fn [] (set! (.-enabled controls) true)))

    (.appendChild container (.-dom stats))
    (.addEventListener js/window "resize" (partial on-window-resize pixel-size) false)
    (on-window-resize pixel-size)))

(defn animate
  []
  (let [delta (.getDelta clock)]
    (js/requestAnimationFrame animate)
    (doseq [m @mixers]
      (.update m delta))
    (.update controls delta)
    (when-let [f @gameloop] (f delta))
    (.render renderer scene camera)
    (.update stats)))

