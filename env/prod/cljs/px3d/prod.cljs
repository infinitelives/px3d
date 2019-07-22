(ns px3d.prod
  (:require
    [px3d.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
