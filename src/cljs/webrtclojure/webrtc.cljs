(ns webrtclojure.webrtc
  (:require [webrtclojure.state :as state]))

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


(defn add-peer [peer connection channel]
    (swap! state/connected-peers-atom  assoc :peer {:connection connection :channel channel}))

(defn get-connection [peer]
    (:connection (:peer @state/connected-peers-atom)))

(defn get-channel [peer]
    (:channel (:peer @state/connected-peers-atom)))


;;; -------------------------
;;; Data channel handlers

(defn dc-receive-message! [user]
  (fn [event]
    (let [message (aget event "data")]
      (state/append! state/recvtextarea-atom user message)
      (print "Recived message: " message))))

(defn dc-send-message! [message]
     (print "Sending message: " message)
     (doseq [peer @state/connected-peers-atom]
        (.send (get-channel peer) message)))

(defn pc-on-data-channel! [user]
  (fn [event]
   (let [dc (aget event "channel")]
      (aset dc "onmessage"  (dc-receive-message! user))
      (aset dc "onopen"     #(print "Data channel opened."))
      (aset dc "onclose"    #(print "Data channel closed."))
      (aset dc "onerror"    print))))

;;; -------------------------
;;; Connection peer handlers
(defn process-candidate!
    "Process recived candidate"
    [send-fn sender candidate]
    (.debug js/console "# Recived an candidate, processing.")
    (.addIceCandidate (get-connection sender) (new js/RTCIceCandidate candidate)
                                              #(print "Candidate successfully added.")
                                              print))

(defn pc-on-ice-candidate-fn
    [send-fn sender pc]
    (fn [event]
     (print "New ICE candida.")
     (if (and (not(nil? event)) (not(nil? (aget event "candidate"))))
        (send-fn [:webrtclojure/candidate
                  { :receiver   sender
                    :candidate (.stringify js/JSON (aget event "candidate"))}] 8000))))

;;; -------------------------
;;; Answer handlers
(defn process-answer!
    "Process recived answers"
    [send-fn sender answer]
    (print "Recived an answer, processing.")
    (let [session (new js/RTCSessionDescription nil)
          pc (get-connection sender)]
        (aset session "type" (.-type answer))
        (aset session "sdp"  (.-sdp answer))
        (.setRemoteDescription pc session
                                  #(print "Successfully added local description.")
                                  print)))

(defn answer-success-fn
    "Callback function for newly requested answer"
    [send-fn sender pc]
    (fn [answer]
        (print "Answering offer.")
        (.setLocalDescription pc answer
                                 #(print "Successfully added local description.")
                                 print)
        (send-fn [:webrtclojure/answer  { :receiver sender
                                          :answer   (.stringify js/JSON answer)}] 8000)))

;;; -------------------------
;;; Offer handlers
(defn offer-success-fn
    "Callback function for newly requested offer"
    [send-fn sender pc]
    (fn [offer]
        (print "Sending requested offer.")
         (.setLocalDescription pc offer
                                 #(print "Successfully added local description.")
                                 print)
        (send-fn [:webrtclojure/offer { :receiver   sender
                                        :offer     (.stringify js/JSON offer)}] 8000)))

(defn session-success-fn
    "Callback function for newly created sessions"
    [send-fn sender pc]
    (fn []
        (print "New session created")
        (.createAnswer pc
                       (answer-success-fn send-fn sender pc)
                       print
                       sdp-constraints)))

(defn process-offer!
    "Process newly recived offers"
    [send-fn sender offer]
    (print "Recived an offer, processing.")

    (let [pc (new js/webkitRTCPeerConnection pc-configuration)
          dc (.createDataChannel pc nil dc-configuration)]
        (aset dc "onmessage"  (dc-receive-message! sender))
        (aset dc "onopen"     #(print "Data channel opened."))
        (aset dc "onclose"    #(print "Data channel closed."))
        (aset dc "onerror"    print)

        (aset pc "onicecandidate" (pc-on-ice-candidate-fn send-fn sender pc))
        (aset pc "ondatachannel"  (pc-on-data-channel! sender))

        (let [session (new js/RTCSessionDescription nil)]
            (aset session "type" (.-type offer))
            (aset session "sdp"  (.-sdp offer))
            (.setRemoteDescription pc session
                                      (session-success-fn send-fn sender pc)
                                      print))
        (add-peer sender pc dc)))

;;; -------------------------
;;; New user handlers

(defn process-new-user!
    "Process new users"
    [send-fn sender]
    (print "New user, processing.")
    (let [pc (new js/webkitRTCPeerConnection pc-configuration)
          dc (.createDataChannel pc nil dc-configuration)]
        (aset dc "onmessage"  (dc-receive-message! sender))
        (aset dc "onopen"     #(print "Data channel opened."))
        (aset dc "onclose"    #(print "Data channel closed."))
        (aset dc "onerror"    print)

        (aset pc "onicecandidate" (pc-on-ice-candidate-fn send-fn sender pc))
        (aset pc "ondatachannel"  (pc-on-data-channel! sender))

        (.createOffer pc (offer-success-fn send-fn sender pc)
                         print
                         sdp-constraints)

        (add-peer sender pc dc)))
