(ns webrtclojure.server-comms
  (:require [taoensso.sente :as sente]))


;;; ------------------------
;;; Set up the Sente channel to the server.

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/sente" ; Must match server side route.
                                  {:type           :auto ; e/o #{:auto :ajax :ws}
                                   :wrap-recv-evs? false})] ; Auto-unwrap :chsk/recv
  (def channel         chsk)    ; Sentes pseudo socket, channel-socket.
  (def receive-channel ch-recv) ; ChannelSocket's receive channel.
  (def channel-send!   send-fn) ; ChannelSocket's send API fn.
  (def channel-state   state)   ; Watchable, read-only atom.
)

;;; -------------------------
;;; Message handers for application routes
(def handle-broadcast   nil)
(def handle-offer       nil)
(def handle-answer      nil)
(def handle-candidate   nil)

(defn set-message-handlers! 
  "Sets messages handlers:
  :broadcast  ; Broadcast message handler
  :offer      ; Offer message handler"
  [& {:keys [broadcast offer answer candidate]
      :as   opts
      :or   {broadcast       nil
             offer           nil
             answer          nil
             candidate       nil}}] 
  {:pre  [(not (nil? broadcast))
          (not (nil? offer))
          (not (nil? answer))
          (not (nil? candidate))]}
  
  (set! handle-broadcast broadcast)
  (set! handle-offer offer)
  (set! handle-answer answer)
  (set! handle-candidate candidate))

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
    (.debug js/console "Channel socket successfully established!")
    (.debug js/console "Channel socket state change: %s" ?data)))

(defmethod -message-handler :chsk/handshake
  ;; Handshake for WS
  [{:keys [?data]}]
  (let [[?uid] ?data]
    (.debug js/console "Handshake done for: %s" ?uid)))

;;; Application specific routes

(defmethod -message-handler :webrtclojure/broadcast
  [{:as ev-msg :keys [?data]}]
  ;; Parse the SDP object
  (def data (.parse js/JSON (get-in (:event ev-msg) [1])))
  
  ;; Process the broadcast
  (handle-offer data))

(defmethod -message-handler :webrtclojure/offer
  [{:as ev-msg :keys [?data]}]
  ;; Verify that we are not singaling our self
  ;; Or move it to sever to not send to sender.

  ;; Parse the SDP object
  (def offer-sdp (.parse js/JSON (get-in (:event ev-msg) [1 :offer])))
  
  ;; Process the offer
  (handle-offer offer-sdp))

(defmethod -message-handler :webrtclojure/answer
  [{:as ev-msg :keys [?data]}]
  ;; Verify that we are not singaling our self
  ;; Or move it to sever to not send to sender.

  ;; Parse the SDP object
  (def answer-sdp (.parse js/JSON (get-in (:event ev-msg) [1 :answer])))
  
  ;; Process the answer
  (handle-answer answer-sdp))

(defmethod -message-handler :webrtclojure/candidate
  [{:as ev-msg :keys [?data]}]
  ;; Verify that we are not singaling our self
  ;; Or move it to sever to not send to sender.

  ;; Parse the SDP object
  (def candidate (.parse js/JSON (get-in (:event ev-msg) [1 :candidate])))
  
  ;; Process the answer
  (handle-candidate candidate))


;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
;; Stop router if it exists.
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-client-chsk-router! receive-channel message-handler)))



;;; -------------------------
;;; Messages to the server

(defn anonymous-login "Tell the server about an anonymous user" [username]
  (channel-send! [:webrtclient/anonymous-login {:username username}])
  (.info js/console "Sending anonymous login for " username))

(defn login "Login to the server." [username password]
  (channel-send! [:webrtclient/login {:username username :password password}]))

(defn register "Permanently register your username with a password"
  [username email password]
  (channel-send! [:webrtclient/register
                  {:username username :password password :email email}]))
