(ns subsonic-beets.sente-routes
  (:require [subsonic-beets.state :as state]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]))

;;; -------------------------
;;; Setup
(let [uid-counter (atom 0)
      {:keys [ch-recv send-fn ajax-post-fn
              ajax-get-or-ws-handshake-fn
              connected-uids user-id-fn]}
      (sente/make-channel-socket! sente-web-server-adapter
                                  {:user-id-fn (fn [_] (swap! uid-counter inc))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def receive-channel               ch-recv)
  (def send!                         send-fn)
  (def connected-uids                connected-uids))

;;; -------------------------
;;; Application specific functions

(defn broadcast-to-all-but-one
  "Send a broadcast to all users except the user who's the origin of the broadcast."
  [identifier-kw data origin-uid]
  (doseq [uid (:any @connected-uids)]
      (if (not= uid origin-uid)
        (send! uid [identifier-kw data] 8000))))

(defn broadcast
  [identifier-kw data]
  (doseq [uid (:any @connected-uids)]
    (send! uid [identifier-kw data] 8000)))


;;; -------------------------
;;; Routes

(defmulti -message-handler "Entry point for messages over sente." :id)

;;; Non-application specific routes

(defn message-handler
  "Wraps `-message-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-message-handler ev-msg))

;; Unhandled message.
(defmethod -message-handler :default
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

;; Triggers when a particular user connects and wasn't previously connected .
(defmethod -message-handler :chsk/uidport-open
  [{:keys [uid client-id]}]
  (println "New user:" uid client-id)
  (send! uid [::chat-backlog @state/messages]))

;; Ping from clients. Apparently to check that socket is alive.
(defmethod -message-handler :chsk/ws-ping [_])
;; When a user disconnects.
(defmethod -message-handler :chsk/uidport-close
  [data]
  (println "User disconnected:" data))


;;; Application specific routes, the interesting stuff
(defmethod -message-handler :subsonic-beets.server-comms/send-chat
  [{:keys [uid event ?data]}]
  (let [messages (swap! state/messages
                        #(conj % (assoc ?data :uid uid :message-id (count %))))
        message (last messages)]
    (println message)
    (broadcast ::new-message message)))

(defmethod -message-handler :subsonic-beets.server-comms/ack-entry
  [{message-id :?data}]
  (let [messages (swap! state/messages
                        #(assoc % message-id (-> (get % message-id)
                                                 (assoc :acked "acked "))))
        message (get messages message-id)]
    (broadcast ::modified-message message)))


;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
(defn stop-router!  []
  (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (reset! router (sente/start-server-chsk-router! receive-channel message-handler)))
(defn restart-router! [_]
  (stop-router!)
  (start-router!))

(defonce is-router-started? (start-router!))
