(ns leif-comm.server-comms
  (:require [taoensso.sente      :as sente]
            [ajax.core :refer [GET POST]] ; Only for testing
            [leif-comm.webrtc :as webrtc]
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


;;; ------------------------
;;; State
(defonce registry-result (atom ""))


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
  (.error js/console "Unhandled event: %s" event))

(defmethod -message-handler :chsk/state
  ;; Indicates when Sente is ready client-side.
  [{:keys [?data]}]
  (if (= ?data {:first-open? true})
    (print "Channel socket opened: %s" (clj->js ?data))))

(defmethod -message-handler :chsk/handshake
  ;; Handshake for WS
  [{:keys [?data]}]
  (let [[uid csrf-token] ?data]
    (print "Handshake gotten with uid:" uid "and csrf:" csrf-token)))


;;; Application specific routes

(defmethod -message-handler :leif-comm/new-user
  [{:as ev-msg :keys [event uid ?data]}]
  (webrtc/process-new-user! send! (:user ?data) (:nickname ?data)))

(defmethod -message-handler :leif-comm/offer
  [{:as ev-msg :keys [event uid ?data]}]
  (webrtc/process-offer! send! (:sender ?data) (:nickname ?data) (.parse js/JSON (:offer ?data))))

(defmethod -message-handler :leif-comm/answer
  [{:as ev-msg :keys [event uid ?data]}]
  (webrtc/process-answer! send! (:sender ?data) (.parse js/JSON (:answer ?data))))

(defmethod -message-handler :leif-comm/candidate
  [{:as ev-msg :keys [event uid ?data]}]
  (webrtc/process-candidate! send! (:sender ?data) (.parse js/JSON (:candidate ?data))))


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

(defn anonymous-login! "Log in as a returning anonymous user." [nickname]
  (send! [:webrtclient/anonymous-login {:nickname nickname}])
  (.info js/console "Sending anonymous login for" nickname))

(defn login! "Login to the server." [email password]
  (send! [:webrtclient/login {:email email :password password}]))

(defn register! "Permanently register your email with a password"
  [email password]
  ;; TODO: Add error handling.
  (send! [:webrtclient/register {:password password :email email}]
                 5000
                 (fn [reply]
                   (reset! registry-result
                           (if (string? reply) ; If failure message string.
                             reply
                             "You have been registered!")))))

(defn send-message!
  "Send a message to the chat room"
  [text]
  (send! [::send-message {:text text}]))
