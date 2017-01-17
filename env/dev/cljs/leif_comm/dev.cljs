(ns ^:figwheel-no-load leif-comm.dev
  (:require [leif-comm.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)
(println "We're actually running this file")

(core/init!)
