(ns webrtclojure.broadcasting
  (:require [clojure.core.async :as async
            :refer (<! <!! >! >!! put! chan go go-loop)]))

(defn broadcast! "Send a broadcast to all users" [nspace data uids send!]
  (println "Broadcast to namespace: " nspace "\n")
  (doseq [uid (:any @uids)]
  	;; TODO: Should not broadcast to "broadcaster"
    (send! uid [nspace data])))


(defn start-broadcaster! "Broadcast some data to all clients." [uids send!]
    (go-loop [i 0]
      (<! (async/timeout 10000))
      (broadcast! :webrtclojure/broadcast i uids send!)
      (recur (inc i))))

