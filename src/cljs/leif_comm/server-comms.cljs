(ns leif-comm.server-comms
  (:require [taoensso.sente      :as sente]
            [leif-comm.state :as state]
            [reagent.core :as reagent :refer [atom]]))


;;; ------------------------
;;; Set up the Sente channel to the server.

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/sente" ; Must match server side route.
                                  {:type           :auto ; e/o #{:auto :ajax :ws}
                                   :wrap-recv-evs? false})] ; Auto-unwrap :chsk/recv
  (def channel         chsk)    ; Sentes pseudo socket, channel-socket.
  (def receive-channel ch-recv) ; ChannelSocket's receive channel.
  (def send!   send-fn) ; ChannelSocket's send API fn.
  (def channel-state   state))  ; Watchable, read-only atom.

;;; -------------------------
;;; Routes

(defmulti -message-handler "Entry point for messages over sente." :id)

;;; Non-application specific routes

(defn message-handler
  "Wraps `-message-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-message-handler ev-msg))

(defmethod -message-handler :default ; Unhandled message.
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod -message-handler :chsk/state
  ;; Indicates when Sente is ready client-side.
  [{:keys [?data]}]
  (if (= ?data {:first-open? true})
    (println "Channel socket opened: " ?data)))

(defmethod -message-handler :chsk/handshake
  ;; Handshake for WebSocket or long-poll.
  [{:keys [?data]}]
  (let [[uid csrf-token] ?data]
    (println "Handshake gotten with uid:" uid "and csrf:" csrf-token)))


;;; Application specific routes

(defmethod -message-handler :leif-comm.sente-routes/new-message
  [{:keys [?data]}]
  (swap! state/chat-log conj {(:message-id ?data) ?data}))

(defmethod -message-handler :leif-comm.sente-routes/chat-backlog
  [{:keys [?data]}]
  (reset! state/chat-log (into {} (map (juxt :message-id #(assoc % :backlog "backlog ")) ?data))))

(defmethod -message-handler :leif-comm.sente-routes/modified-message
  [{:keys [?data]}]
  (println ?data)
  (swap! state/chat-log assoc (:message-id ?data) ?data))



;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
;; Stop router if it exists.
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router (sente/start-client-chsk-router! receive-channel
                                                  message-handler)))


;;; -------------------------
;;; Messages to the server

(defn anonymous-login!
  "Log in as an anonymous user without username and password."
  [nickname]
  (send! [:webrtclient/anonymous-login {:nickname nickname}])
  (.info js/console "Sending anonymous login for" nickname))

(defn send-message!
  "Send a message to the chat room"
  [text]
  (send! [::send-chat {:text text :author @state/name-atom}]))

(defn ack-entry
  [message-id]
  (send! [::ack-entry message-id]))
