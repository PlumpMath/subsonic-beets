(ns webrtclojure.sente-routes
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]))

;;; -------------------------
;;; Setup

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids user-id-fn]}
      (sente/make-channel-socket! sente-web-server-adapter
                                  {:user-id-fn  (fn [ring-req] (str (get-in ring-req [:session :base-user-id]) "/" (:client-id ring-req))) })]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def receive-channel               ch-recv) ; ChannelSocket's receive channel
  (def channel-send!                 send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

;;; -------------------------
;;; General
(defn broadcast 
  "Send a broadcast to all users except caller" 
  [func data caller]
  (doseq [uid (:any @connected-uids)]
      (if (not= uid caller)
        (channel-send! uid [func data] 8000))))

(defn broadcast-new-user 
  [user]
  "Broadcast the newly connected user"
  (broadcast :webrtclojure/new-user {:user user} user))

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

;; Ping from clients. Who sends this and why?
(defmethod -message-handler :chsk/ws-ping [something]
  (println "We got a ws-ping"))

;;; Application specific authentication routes
(defmethod -message-handler :webrtclient/anonymous-login
  [{:keys [uid ?data]}]
  (println ?data)
  (println "STUB: Hook up anonymous-login to a database.")
  (broadcast-new-user uid))

(defmethod -message-handler :webrtclient/login
  [{:keys [uid ?data]}]
  (println ?data)
  (println "STUB: Hook up login to a database.")
  (broadcast-new-user uid))

(defmethod -message-handler :webrtclient/register
  [{:as ev-msg :keys [?data]}]
  (println ?data)
  (println "STUB: Hook up register to a database."))

;;; Application specific WebRTC routes
(defmethod -message-handler :webrtclojure/offer
  [{:keys [uid event ?data]}]
  (println "Server received an offer, processing ")
  (channel-send!  (:receiver (get-in event[1])) 
                  [:webrtclojure/offer  {:sender uid
                                          :offer  (:offer (get-in event[1]))}]))

(defmethod -message-handler :webrtclojure/answer
  [{:keys [uid event ?data]}]
  (println "Server received an answer, processing ")
  (channel-send!  (:receiver (get-in event[1])) 
                  [:webrtclojure/answer  {:sender uid
                                          :answer  (:answer (get-in event[1]))}]))

(defmethod -message-handler :webrtclojure/candidate
  [{:keys [uid event ?data]}]
  (println "Server received an candidate, processing ")
  (channel-send!  (:receiver (get-in event[1])) 
                  [:webrtclojure/candidate  {:sender uid
                                             :candidate  (:candidate (get-in event[1]))}]))

;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
(defn  stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-server-chsk-router! receive-channel message-handler)))

(defonce is-router-started? (start-router!))
