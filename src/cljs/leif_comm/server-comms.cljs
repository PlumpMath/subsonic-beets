(ns leif-comm.server-comms
  (:require [taoensso.sente      :as sente]
            [ajax.core :refer [GET POST]] ; Only for testing
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
  (println "Unhandled event: %s" event))

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

(defn send-message!
  "Send a message to the chat room"
  [text]
  (send! [::send-message {:text text :author @state/name-atom}]))
