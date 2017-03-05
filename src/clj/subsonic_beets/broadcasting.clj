(ns subsonic-beets.broadcasting
  (:require [clojure.core.async :refer [<! go-loop timeout]]
            [subsonic-beets.sente-routes :refer [send!]]))

(defn broadcast! "Send a broadcast to all users" [func data uids]
  (println "Broadcast to func: " func "\n")
  (doseq [uid (:any @uids)]
  	;; TODO: Should not broadcast to "broadcaster"
    (send! uid [func data])))

(defn start-broadcaster! "Broadcast some data to all clients." [uids send!]
    (go-loop [i 0]
      (<! (timeout 10000))
      (broadcast! :subsonic-beets/broadcast i uids)
      (recur (inc i))))
