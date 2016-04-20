(ns webrtclojure.server-comms
  (:require [taoensso.sente  :as sente]))


;;; ------------------------
;;; Sente

;; Set up the channel to the server.
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/sente" ; Must match server side route.
                                  {:type :auto ; e/o #{:auto :ajax :ws}
                                   :wrap-recv-evs? false})] ; Auto-unwrap :chsk/recv
  (def channel         chsk)    ; Sentes pseudo socket, channel-socket.
  (def receive-channel ch-recv) ; ChannelSocket's receive channel.
  (def channel-send!   send-fn) ; ChannelSocket's send API fn.
  (def channel-state   state)   ; Watchable, read-only atom.
  )

;;; "Routes" for messages passed over Sente channels.
(defmulti -message-handler "Entry point for messages over sente." :id)

(defn message-handler
  "Wraps `-message-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-message-handler ev-msg))

(defmethod -message-handler :default ; Unhandled message.
  [{:as ev-msg :keys [event]}]
  (.error js/console "Unhandled event: %s" event))

(defmethod -message-handler :some/broadcast
  [{:as ev-msg :keys [?data]}]
  (.debug js/console "We received: %s" ev-msg))

(defmethod -message-handler :chsk/state
  ;; Indicates when Sente is ready client-side.
  [{:keys [?data]}]
  (if (= ?data {:first-open? true})
    (.debug js/console "Channel socket successfully established!")
    (.debug js/console "Channel socket state change: %s" ?data)))

(defmethod -message-handler :chsk/handshake
  ;; Handshake for WS
  [{:keys [?data]}]
  (let [[?uid] ?data]
        (.debug js/console "Handshake done for: %s" ?uid)))


(defonce router (atom nil))
;; Stop router if it exists.
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-client-chsk-router! receive-channel message-handler)))


(defonce is-router-started? (start-router!))
