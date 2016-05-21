(ns webrtclojure.sente-routes
  (:require [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit
             :refer [sente-web-server-adapter]]
            [webrtclojure.accounts :as accounts]))

;;; -------------------------
;;; Setup

(let [{:keys [ch-recv send-fn ajax-post-fn
              ajax-get-or-ws-handshake-fn
              connected-uids user-id-fn]}
      (sente/make-channel-socket! sente-web-server-adapter
                                  {:user-id-fn
                                   (fn [_] (:id (accounts/create-anonymous-user!)))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  ;; ChannelSocket's receive channel.
  (def receive-channel               ch-recv)
  ;; ChannelSocket's send API func.
  (def channel-send!                 send-fn)
  ; Watchable, read-only atom.
  (def connected-uids                connected-uids))

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

;; Ping from clients. Apparently to check that socket is alive.
(defmethod -message-handler :chsk/ws-ping [_])


;;; Application specific authentication routes
(defmethod -message-handler :webrtclient/anonymous-login
  [{:keys [uid user ?data]}]
  (println "Updating account for" user)
  (accounts/update-user! uid user)
  (broadcast-new-user (:id user)))

(defmethod -message-handler :webrtclient/login
  [{:keys [uid ?data]}]
  (println ?data)
  (println "STUB: Hook up login to a database.")
  (broadcast-new-user uid))

(defmethod -message-handler :webrtclient/register
  [{:keys [uid ?data ?reply-fn]}]
  ;; Reply back to the caller with the result of the insert.
  (?reply-fn
   (if (< (count  (:password ?data)) 7)
     "Password too short."
     (if (not (re-matches #".+@.+" (:email ?data)))
       "Invalid email adress."
       (accounts/update-user! uid ?data)))))


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
