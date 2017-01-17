(ns leif-comm.sente-routes
  (:require [leif-comm.state :as state]
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
;;; General
(defn broadcast
  "Send a broadcast to all users except caller"
  [func data caller]
  (doseq [uid (:any @connected-uids)]
      (if (not= uid caller)
        (send! uid [func data] 8000))))

(defn broadcast-new-user
  [uid nickname]
  "Broadcast the newly connected user"
  (broadcast :leif-comm/new-user {:user uid :nickname nickname} uid))

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
  (println "Unhandled event: %s" event))

;; Triggers when a particular user connects and wasn't previously connected .
(defmethod -message-handler :chsk/uidport-open
  [{:keys [uid client-id]}]
  (println "New user:" uid client-id))

;; Ping from clients. Apparently to check that socket is alive.
(defmethod -message-handler :chsk/ws-ping [_])





;;; Application specific WebRTC routes
(defmethod -message-handler :leif-comm/offer
  [{:keys [uid event ?data]}]
  (println "Server received an offer, processing ")
  (send!  (:receiver ?data)
                  [:leif-comm/offer {:sender uid :offer (:offer ?data) :nickname (:nickname ?data)}]))

(defmethod -message-handler :leif-comm/answer
  [{:keys [uid event ?data]}]
  (println "Server received an answer, processing ")
  (send!  (:receiver ?data)
                  [:leif-comm/answer {:sender uid :answer (:answer ?data)}]))

(defmethod -message-handler :leif-comm/candidate
  [{:keys [uid event ?data]}]
  (println "Server received an candidate, processing ")
  (send!  (:receiver ?data)
                  [:leif-comm/candidate {:sender uid :candidate (:candidate ?data)}]))

(defmethod -message-handler :leif-comm.server-comms/send-message
  [{:keys [uid event ?data]}]
  (swap! state/messages conj (assoc ?data :uid uid))
  (println @state/messages))

;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
(defn stop-router!  []
  (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-server-chsk-router! receive-channel message-handler)))

(defonce is-router-started? (start-router!))
