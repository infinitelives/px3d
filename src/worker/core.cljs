(ns worker.core
  (:require ["rot-js" :as rot]))

(print "HELLO I AM WORKER")

(rot/RNG.setSeed 1000)
(js/console.log "RNG" (rot/RNG.getPercentage))

(let [s (rot/Noise.Simplex.)]
  (js/console.log (.get s 1 1)))

(set! js/onmessage
      (fn [e]
        (js/console.log "Message received" e)
        (js/postMessage "Hello")))
