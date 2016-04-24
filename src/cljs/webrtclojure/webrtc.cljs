(ns webrtclojure.webrtc
	(:require [webrtclojure.server-comms :as server-comms]
              ))

;;; ------------------------
;;; WebRTC

;; Local peer connection
(def ^:dynamic pc nil)

;; Contant configurations
(def ^:const pc-configuration #js {
	"iceServers" #js [ 
		#js { "url" "stun:stun.l.google.com:19302" }
		#js { "url" "stun:stun1.l.google.com:19302" } ]})

(def ^:const dc-configuration  #js {
	:ordered false				; use UDP
	:protocol "SCTP"
	:maxRetransmitTime 1000 })	

(def ^:const dc-sdp-constraints #js {
	"mandatory" #js {
		"OfferToReceiveAudio" false
		"OfferToReceiveVideo" false } })

;; PC and DC handlers
(defn onsignalingstatechange! [state]
    (.debug js/console "## Signaling state change: %s" state))

(defn oniceconnectionstatechange! [state]
    (.debug js/console "## Ice connection state change: %s" state))

(defn onicegatheringstatechange! [state]
    (.debug js/console "## Ice gathering state change: %s" state))

(defn onicecandidate! [state]
    (.debug js/console "## Ice candidate state change: %s" state))

(defn ondatachannel! [state]
    (.debug js/console "## Date channel state change: %s" state))

(defn onaddstream! [state]
    (.debug js/console "## Add stream state change: %s" state))

(defn onremovestream! [state]
    (.debug js/console "## Remove stream state change: %s" state))

(defn onnegotiationneeded! [state]
    (.debug js/console "## Negotiation needed state change: %s" state))

(defn onmessage! [state]
    (.debug js/console "## DC onmessage state change: %s" state))

(defn onopen! [state]
    (.debug js/console "## DC onopen state change: %s" state))

(defn onclose! [state]
    (.debug js/console "## DC onclose state change: %s" state))

(defn onerror! [state]
    (.debug js/console "## DC onerror state change: %s" state))

(defn offer-success! [sdp]
    (.debug js/console "## offer-success")
    (.setLocalDescription pc sdp)

    ;; Signal
    (server-comms/channel-send! 
    	[:webrtclojure/signal
    	 (.stringify js/JSON  
    	 		{:from 		pc 			 ;; example data, some attributes might not be necessary
	       		 :action	"offer"
	       		 :data 		sdp})]
	     8000))					 ;; timeout
	       

(defn offer-failure! [sdp]
    (.debug js/console "## offer-failure" sdp))

(defn create-data-connection! []

    ;; Create a local peer connection
	(set! pc (new js/webkitRTCPeerConnection pc-configuration))
	(aset pc "onsignalingstatechange" 		onsignalingstatechange!)
	(aset pc "oniceconnectionstatechange" 	oniceconnectionstatechange!)
	(aset pc "onicegatheringstatechange" 	onicegatheringstatechange!)
	(aset pc "onicegatheringstatechange" 	onicegatheringstatechange!)
	(aset pc "onicecandidate" 				onicecandidate!)
	(aset pc "ondatachannel" 				ondatachannel!)
	(aset pc "onaddstream" 					onaddstream!)
	(aset pc "onnegotiationneeded" 			onnegotiationneeded!)
	(aset pc "onremovestream" 				onremovestream!)

	;; Open a data channel to recive new peers
	;;(def channel (.createDataChannel pc "dc" dc-configuration))
	;;(aset channel "onmessage" 	onmessage!)
	;;(aset channel "onopen" 		onopen!)
	;;(aset channel "onclose" 	onclose!)
	;;(aset channel "onerror" 	onerror!)

	;; Create an offer 
	(def offer (.createOffer pc offer-success! 
								offer-failure! 
								dc-sdp-constraints))
)

