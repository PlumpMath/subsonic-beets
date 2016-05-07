(ns webrtclojure.webrtc-wrapper)

;;; ------------------------
;;; WebRTC wrapper

;; Contant configurations
;; TODO: move to global settings?
(def ^:const pc-configuration #js {
	"iceServers" #js [ 
		#js { "url" "stun:stun.l.google.com:19302" }
		#js { "url" "stun:stun1.l.google.com:19302" } ]})

(def ^:const dc-configuration  #js {
	:ordered false				; use UDP
	:protocol "SCTP"
	:maxRetransmitTime 1000 })	

(def ^:const sdp-constraints #js {
	"mandatory" #js {
		"OfferToReceiveAudio" false
		"OfferToReceiveVideo" false } })

(defn create-peer-connection! 
	"Creates a RTCPeerConnection:
	:pc-configuration 	; Peer connection configuration.
	@returns newly created RTCPeerConnection"
	[& { :keys [configuration]
       	 :as   opts
       	 :or   {configuration 	pc-configuration}}]
       	 
	(new js/webkitRTCPeerConnection configuration))

(defn set-peer-connection-callback! 
	"Set RTCPeerConnection callback functions:
	:pc 				; Peer connection
	:onicecandidate 	; A callback funtion will be called on ice event
	:ondatachannel 		; A callback funtion will be called on data channel event
	@returns none"
	[& { :keys [pc onicecandidate ondatachannel ]
       	 :as   opts
       	 :or   {pc 				nil
       	 		onicecandidate  nil
       	 		ondatachannel   nil }}]
    {:pre  [(not (nil? pc))]}

    (aset pc "onicecandidate"	onicecandidate)
    (aset pc "ondatachannel"	ondatachannel))

(defn create-ice-candidate! 
	"Creates a RTCPeerConnection:
	:candidate 	; Candidate event
	@returns newly created RTCIceCandidate"
	[& { :keys [candidate]
       	 :as   opts
       	 :or   {candidate 	nil}}]
    {:pre  [(not (nil? candidate))]}
       	 
	(new js/RTCIceCandidate candidate))

(defn add-peer-ice-candidate! 
	"Creates a RTCPeerConnection:
	:pc 				; Peer connection
	:success-callback 	; A callback funtion, will be called on success
	:failure-callback 	; A callback funtion, will be called on failure
	:candidate 			; An ice candidate object
	@returns none"
	[& { :keys [pc candidate success-callback failure-callback]
       	 :as   opts
       	 :or   {pc 				nil
       	 		candidate  		nil
       	 		success-callback	nil
       	 		failure-callback	nil}}]
    {:pre  [(not (nil? pc))
    	 	(not (nil? candidate))]}

    (.addIceCandidate pc candidate 
    	  				 success-callback 
    					 failure-callback))

(defn create-offer! 
	"Initiates the creation of an SDP offer:
	:pc 				; Peer connection
	:success-callback 	; A callback funtion, will be called on success
	:failure-callback 	; A callback funtion, will be called on failure
	:options 			; An RTCOfferOptions object providing options requested for the offer
	@returns A Promise object which, when the offer has been created, is resolved with a 
			   RTCSessionDescription object containing the newly-created offer"
	[& { :keys [pc success-callback failure-callback options]
		 :as   opts
       	 :or   {pc 					nil
       	 		success-callback	nil
       	 		failure-callback	nil
       	 		options 			sdp-constraints}}] 
	{:pre  [(not (nil? pc))
			(not (nil? success-callback))]}
	
	(.createOffer pc success-callback 
 					 failure-callback 
 					 options))

(defn create-answer! 
	"Creates an answer to an offer received from a remote peer during the offer/answer 
	negotiation of a WebRTC connection:
	:pc 				; Peer connection
	:success-callback 	; A callback funtion, will be called on success
	:failure-callback 	; A callback funtion, will be called on failure
	:options 			; An RTCAnswerOptions object providing options requested for the offer
	@returns A Promise whose fulfillment handler is called with a RTCSessionDescription object 
			 containing SDP which describes the generated answer"
	[& { :keys [pc success-callback failure-callback options]
		 :as   opts
       	 :or   {pc 					nil
       	 		success-callback	nil
       	 		failure-callback	nil
       	 		options 			sdp-constraints}}] 
	{:pre  [(not (nil? pc))
			(not (nil? success-callback))]}
	
	(.createAnswer pc success-callback 
 					 failure-callback 
 					 options))

(defn create-data-channel! 
	"Creates a new data channel on the peer connection:
	:pc 			; Peer connection
	:configuration 	; Data channel configuration
	:label 			; A human-readable name for the channel
	@returns newly created RTCPeerConnection"
	[& { :keys [pc configuration label]
		 :as   opts
       	 :or   {pc 				nil
       	 		configuration 	dc-configuration
       	 		label			nil }}] 
	{:pre  [(not (nil? pc))]}
	
	(.createDataChannel pc label configuration))

(defn set-data-channel-callback! 
	"Set RTCDataChannel callback functions:
	:dc 				; Data channel
	:onmessage 			; A callback funtion will be called on message arrival
	:onopen 			; A callback funtion will be called on open
	:onclose 			; A callback funtion will be called on close
	:onerror 			; A callback funtion will be called on error
	@returns none"
	[& { :keys [dc onmessage onopen onclose onerror]
       	 :as   opts
       	 :or   {dc  		nil
       	 		onmessage  	nil
       	 		onopen   	nil
       	 		onclose   	nil
       	 		onerror   	nil }}]
    {:pre  [(not (nil? dc))]}

    (aset dc "onmessage"	onmessage)
    (aset dc "onopen"		onopen)
    (aset dc "onclose"		onclose)
    (aset dc "onerror"		onerror))


(defn create-session-description! 
	"Creates a new session description:
	:offer			; offer
	@returns newly created RTCPeerConnection"
	[& { :keys [offer]
       	 :as   opts
       	 :or   {offer 	nil}}]
	(new js/RTCSessionDescription offer))

(defn set-local-description! 
	"Set local description associated with the connection:
	:pc 					; Peer connection
	:session-description	; Session description
	:success-callback 		; A callback funtion, will be called on success.
	:failure-callback 		; A callback funtion, will be called on failure.
	@returns A Promise object which is fulfilled when the local description is changed or failed."
	[& { :keys [pc session-description success-callback failure-callback]
		 :as   opts
       	 :or   {pc 					nil
       	 		session-description nil
       	 		success-callback	nil
       	 		failure-callback	nil}}] 
	{:pre  [(not (nil? pc))
			(not (nil? session-description))]}
	
	(.setLocalDescription pc  session-description 
							  success-callback
							  failure-callback))

(defn set-remote-description! 
	"Set remote description associated with the connection:
	:pc 					; Peer connection
	:session-description	; Session description
	:success-callback 		; A callback funtion, will be called on success.
	:failure-callback 		; A callback funtion, will be called on failure.
	@returns A Promise object which is fulfilled when the remote description is changed or failed."
	[& { :keys [pc session-description success-callback failure-callback]
		 :as   opts
       	 :or   {pc 					nil
       	 		session-description nil
       	 		success-callback	nil
       	 		failure-callback	nil}}] 
	{:pre  [(not (nil? pc))
			(not (nil? session-description))]}
	
	(.setRemoteDescription pc session-description 
							  success-callback
							  failure-callback))

