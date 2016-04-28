(ns webrtclojure.broadcasting
  (:require [clojure.core.async :refer [<! go-loop timeout]]
            [webrtclojure.sente-routes :refer [channel-send!]]))

(defn broadcast! "Send a broadcast to all users" [number uids]
  (println "Doing a broadcast with:\n" number "\n" @uids)
  (doseq [uid (:any @uids)]
    (channel-send! uid
     [:webrtclojure/broadcast
      {:what-is-this "An async broadcast pushed from server"
       :how-often    "Every 10 seconds"
       :to-whom      uid
       :number       number}])))

(defn start-broadcaster! "Broadcast some data to all clients." [uids]
    (go-loop [number 0]
      (<! (timeout 10000))
      (broadcast! number uids channel-send!)
      (recur (inc number))))
