(ns webrtclojure.webrtc)

;;; ------------------------
;;; WebRTC connection handler

(def ^:const pc-configuration #js {
  "iceServers" #js [ 
    #js { "url" "stun:stun.l.google.com:19302" }
    #js { "url" "stun:stun1.l.google.com:19302" } ]})

(def ^:const dc-configuration  #js {
  :ordered false        ; use UDP
  :protocol "SCTP"
  :maxRetransmitTime 1000 })  

(def ^:const sdp-constraints #js {
  "mandatory" #js {
    "OfferToReceiveAudio" false
    "OfferToReceiveVideo" false } })

;; Global
(defonce connected-peers (atom {}))

(defn add-peer [peer connection channel]
    (swap! connected-peers  assoc :peer {:connection connection :channel channel})) 

(defn get-connection [peer]
    (:connection (:peer @connected-peers)))

(defn get-channel [peer]
    (:channel (:peer @connected-peers)))

;;; -------------------------
;;; Logger
(defn log-message 
    "Log message"
    [& message]
    (.debug js/console (str "# " message)))

(defn log-message-fn [message] 
     (fn [] (log-message message)))

;;; -------------------------
;;; Data channel handlers

(defn dc-receive-message! [message] 
     (log-message "Recived message: " (aget message "data")))

(defn dc-send-message! [message] 
     (log-message "Sending message: " message)
     (doseq [peer @connected-peers]
        (.send (get-channel peer) message)))

;;; -------------------------
;;; Connection peer handlers
(defn process-candidate! 
    "Process recived candidate"
    [send-fn sender candidate]
    (.debug js/console "# Recived an candidate, processing.")
    (.addIceCandidate (get-connection sender) (new js/RTCIceCandidate candidate)
                                              (log-message-fn "Candidate successfully added.")
                                              log-message-fn))

(defn pc-on-ice-candidate-fn 
    [send-fn sender pc] 
    (fn [event] 
     (log-message "New ICE candida.")
     (if (and (not(nil? event)) (not(nil? (aget event "candidate")))) 
        (send-fn [:webrtclojure/candidate
                  { :receiver   sender
                    :candidate (.stringify js/JSON (aget event "candidate"))}] 8000))))

(defn pc-on-data-channel! [event] 
   (let [dc (aget event "channel")]
      (aset dc "onmessage"  dc-receive-message!)
      (aset dc "onopen"     (log-message-fn "Data channel opened."))
      (aset dc "onclose"    (log-message-fn "Data channel closed."))
      (aset dc "onerror"    log-message-fn)))

;;; -------------------------
;;; Answer handlers
(defn process-answer! 
    "Process recived answers"
    [send-fn sender answer]
    (log-message "Recived an answer, processing.")
    (let [session (new js/RTCSessionDescription nil)
          pc (get-connection sender)]  
        (aset session "type" (.-type answer))
        (aset session "sdp"  (.-sdp answer))
        (.setRemoteDescription pc session
                                  (log-message-fn "Successfully added local description.")
                                  log-message-fn)))

(defn answer-success-fn
    "Callback function for newly requested answer"
    [send-fn sender pc]
    (fn [answer]
        (log-message "Answering offer.")
        (.setLocalDescription pc answer
                                 (log-message-fn "Successfully added local description.")
                                 log-message-fn)
        (send-fn [:webrtclojure/answer  { :receiver sender
                                          :answer   (.stringify js/JSON answer)}] 8000)))

;;; -------------------------
;;; Offer handlers
(defn offer-success-fn 
    "Callback function for newly requested offer"
    [send-fn sender pc] 
    (fn [offer]
        (log-message "Sending requested offer.")
         (.setLocalDescription pc offer
                                 (log-message-fn "Successfully added local description.")
                                 log-message-fn)
        (send-fn [:webrtclojure/offer { :receiver   sender
                                        :offer     (.stringify js/JSON offer)}] 8000)))

(defn session-success-fn 
    "Callback function for newly created sessions"
    [send-fn sender pc] 
    (fn []
        (log-message "New session created")
        (.createAnswer pc 
                       (answer-success-fn send-fn sender pc) 
                       log-message-fn
                       sdp-constraints)))

(defn process-offer! 
    "Process newly recived offers"
    [send-fn sender offer]
    (log-message "Recived an offer, processing.")

    (let [pc (new js/webkitRTCPeerConnection pc-configuration)
          dc (.createDataChannel pc nil dc-configuration)]
        (aset dc "onmessage"  dc-receive-message!)
        (aset dc "onopen"     (log-message-fn "Data channel opened."))
        (aset dc "onclose"    (log-message-fn "Data channel closed."))
        (aset dc "onerror"    log-message-fn)
        
        (aset pc "onicecandidate" (pc-on-ice-candidate-fn send-fn sender pc))
        (aset pc "ondatachannel"  pc-on-data-channel!)
    
        (let [session (new js/RTCSessionDescription nil)]  
            (aset session "type" (.-type offer))
            (aset session "sdp"  (.-sdp offer))
            (.setRemoteDescription pc session
                                      (session-success-fn send-fn sender pc)
                                      log-message-fn))
        (add-peer sender pc dc)))

;;; -------------------------
;;; New user handlers

(defn process-new-user! 
    "Process new users"
    [send-fn sender]
    (log-message "New user, processing.")
    (let [pc (new js/webkitRTCPeerConnection pc-configuration)
          dc (.createDataChannel pc nil dc-configuration)]
        (aset dc "onmessage"  dc-receive-message!)
        (aset dc "onopen"     (log-message-fn "Data channel opened."))
        (aset dc "onclose"    (log-message-fn "Data channel closed."))
        (aset dc "onerror"    log-message-fn)
        
        (aset pc "onicecandidate" (pc-on-ice-candidate-fn send-fn sender pc))
        (aset pc "ondatachannel"  pc-on-data-channel!)

        (.createOffer pc (offer-success-fn send-fn sender pc)
                         log-message-fn
                         sdp-constraints)
        
        (add-peer sender pc dc)))