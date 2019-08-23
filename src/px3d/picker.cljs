(ns px3d.picker)

(defn mouse-pick [ev raycaster container scene camera]
  (let [pos #js {:x (-> ev .-clientX (/ (aget container "clientWidth")) (* 2) (- 1))
                 :y (-> ev .-clientY (/ (aget container "clientHeight")) (* -2) (+ 1))}]
    (.setFromCamera
      raycaster
      pos
      camera)
    (.intersectObjects raycaster (.-children scene) true)))

(defn register [[scene camera renderer] callback]
  "register mouse pick event"
  (let [raycaster (THREE.Raycaster.)
        picker (partial mouse-pick raycaster #js {} scene camera)
        moved (atom false)
        down (fn [ev] (reset! moved [(.-clientX ev) (.-clientY ev)]))
        done (fn [ev]
               (let [[ox oy] @moved
                     nx (.-clientX ev)
                     ny (.-clientY ev)]
                 (when (and (< (js/Math.abs (- ox nx)) 5)
                            (< (js/Math.abs (- oy ny)) 5))
                   (let [picked (#'mouse-pick ev raycaster (aget renderer "domElement") scene camera)]
                     (callback picked nx ny)))))]
    (js/console.log "registering mouse/touch events")
    (.addEventListener js/window "mousedown" down false)
    (.addEventListener js/window "mouseup" done false)
    (.addEventListener js/window "touchstart" (fn [ev] (down (aget ev "changedTouches" 0)) (.preventDefault ev) false) false)
    (.addEventListener js/window "touchend" (fn [ev] (done (aget ev "changedTouches" 0)) (.preventDefault ev) false) false)
    true)
  true)

