(ns subsonic-beets.prod
  (:require [subsonic-beets.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
