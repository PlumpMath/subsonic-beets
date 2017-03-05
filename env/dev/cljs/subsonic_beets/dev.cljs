(ns ^:figwheel-no-load subsonic-beets.dev
  (:require [subsonic-beets.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(core/init!)
