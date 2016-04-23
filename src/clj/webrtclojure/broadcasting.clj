(ns webrtclojure.broadcasting
  (:require [clojure.core.async :as async
            :refer (<! <!! >! >!! put! chan go go-loop)]))

(defn broadcast! "Send a broadcast to all users" [i uids send!]
  (println "Doing a broadcast with:\n" i "\n" @uids)
  (doseq [uid (:any @uids)]
    (send! uid
     [:webrtclojure/broadcast
      {:what-is-this "An async broadcast pushed from server"
       :how-often    "Every 10 seconds"
       :to-whom      uid
       :i            i}])))

(defn start-broadcaster! "Broadcast some data to all clients." [uids send!]
    (go-loop [i 0]
      (<! (async/timeout 10000))
      (broadcast! i uids send!)
      (recur (inc i))))
