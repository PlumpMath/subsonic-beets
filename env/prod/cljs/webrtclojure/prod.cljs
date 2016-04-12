(ns webrtclojure.prod
  (:require [webrtclojure.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
