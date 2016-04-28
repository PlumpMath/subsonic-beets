(ns webrtclojure.broadcasting
  (:require [clojure.core.async :refer [<! go-loop timeout]]
            [webrtclojure.sente-routes :refer [channel-send!]]))

(defn broadcast! "Send a broadcast to all users" [func data uids]
  (println "Broadcast to func: " func "\n")
  (doseq [uid (:any @uids)]
  	;; TODO: Should not broadcast to "broadcaster"
    (channel-send! uid [func data])))

(defn start-broadcaster! "Broadcast some data to all clients." [uids send!]
    (go-loop [i 0]
      (<! (timeout 10000))
      (broadcast! :webrtclojure/broadcast i uids)
      (recur (inc i))))
