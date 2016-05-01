(ns webrtclojure.webrtc
 (:require  [webrtclojure.webrtc-wrapper :as webrtc-wrapper]
 			[webrtclojure.server-comms   :as server-comms])
	)

;;; ------------------------
;;; WebRTC connection handler

;; Global
(def ^:dynamic self-pc nil) 			;; Our local peer connection
(def ^:dynamic self-dc nil) 			;; Our local data channel
(def ^:dynamic conncted-peers []) 		;; Connected peers

;;; -------------------------
;;; Connection peer handlers

(defn candidate-process! 
	"Process recived candidate"
	[candidate]
	(.debug js/console "# Recived an candidate, processing...")

	(webrtc-wrapper/add-peer-ice-candidate! :pc self-pc 
	:candidate (webrtc-wrapper/create-ice-candidate! :candidate candidate)))

(defn pc-on-ice-candidate! [event] 
	 (.debug js/console "## Processing ice candida: %s" event)
	 (if (and (not(nil? event)) (not(nil? (aget event "candidate")))) 
	 	(server-comms/channel-send! [:webrtclojure/candidate
	                            	{:candidate (.stringify js/JSON (aget event "candidate"))}]
	                            	8000)))

(defn pc-on-data-channel! [] 
	 (.debug js/console "## PC Data channel event"))

;;; -------------------------
;;; Answer handlers

(defn answer-process! 
	"Process recived answers"
	[sdp]
	(.debug js/console "# Recived an answer, processing...")

	;; Set the remote description 
	(webrtc-wrapper/set-remote-description! :pc self-pc
	:session-description (webrtc-wrapper/create-session-description! sdp))
	)

(defn answer-success! 
	"Callback function to trigger answer singaling, with the created answer"
	[sdp]
	(.setLocalDescription self-pc sdp)

	;; Signal
	;; TODO: Find a more suitable solution for stringify
	(.debug js/console "# Answering offer...")
	(server-comms/channel-send! [:webrtclojure/answer
	                            {:answer (.stringify js/JSON sdp)}]
	                            8000))  ;; timeout
(defn answer-failure! 
  	"Callback function when the creation of an answer fails"
	[sdp]
    (.debug js/console "# Failed to create an answer")) 

;;; -------------------------
;;; Data channel handlers

(defn dc-on-message! [message] 
	 (.debug js/console "## Recived message on data channel: %s" message))

(defn dc-on-open! []
	 (.debug js/console "## Data channel opened"))

(defn dc-on-close! []
	 (.debug js/console "## Data channel closeed"))

(defn dc-on-error! []
	 (.debug js/console "## Data channel error"))
;;; -------------------------
;;; Offer handlers

(defn offer-process! 
	"Process recived offers"
	[sdp]
	(.debug js/console "# Recived an offer, processing...")

	;; Createa new peer connection for the remote peer
	(set! self-pc (webrtc-wrapper/create-peer-connection!))
	(webrtc-wrapper/set-peer-connection-callback! :pc self-pc
								  				  :onicecandidate pc-on-ice-candidate!
								  				  :ondatachannel  pc-on-data-channel!)

	;; Create a data channel
	(set! self-dc (webrtc-wrapper/create-data-channel! :pc self-pc))
	(webrtc-wrapper/set-data-channel-callback! :dc self-dc
								  				     :onmessage  	dc-on-message!
								  				     :onopen 		dc-on-open!
								  				     :onclose 		dc-on-close!
								  				     :onerror 		dc-on-error!)
	;; Set the remote description 
	(webrtc-wrapper/set-remote-description! :pc self-pc
	:session-description (webrtc-wrapper/create-session-description! sdp))
	
	;; Create an answer
	(webrtc-wrapper/create-answer! :pc self-pc
								   :success-callback answer-success!
								   :failure-callback answer-failure!)

	;; Store the peer connection
	;; TODO: This will be used later, we will also need to 
	;; 		 store uuid of the users, for singaling.
	;;(set! conncted-peers (conj conncted-peers {pc dc}))
	)

(defn offer-success! 
	"Callback function to trigger offer singaling, with the created offer"
	[sdp]
	(.setLocalDescription self-pc sdp)

	;; Signal
	;; TODO: Find a more suitable solution for stringify
	(.debug js/console "# Signaling offer...")
	(server-comms/channel-send! [:webrtclojure/offer
	                            {:offer (.stringify js/JSON sdp)}]
	                            8000))  ;; timeout

(defn offer-failure! 
  	"Callback function when the creation of an offer fails"
	[sdp]
    (.debug js/console "# Failed to create an offer")) 


;;; -------------------------
;;; Braodcast handlers

(defn broadcast-process! 
	"Process recived offers"
	[data]
	(.debug js/console "# Recived new broadcast, processing..."))

;;; -------------------------
;;; Signaling 

(defn initialize! []
	;; Initialize local peer and trigger signaling 
	(set! self-pc (webrtc-wrapper/create-peer-connection!))
	(webrtc-wrapper/set-peer-connection-callback! :pc self-pc
								  				  :onicecandidate pc-on-ice-candidate!
								  				  :ondatachannel  pc-on-data-channel!)
	;; Create a data channel
	(set! self-dc (webrtc-wrapper/create-data-channel! :pc self-pc))
	(webrtc-wrapper/set-data-channel-callback! :dc self-dc
								  				     :onmessage  	dc-on-message!
								  				     :onopen 		dc-on-open!
								  				     :onclose 		dc-on-close!
								  				     :onerror 		dc-on-error!)

	(server-comms/set-message-handlers! :broadcast  broadcast-process!
										:offer 		offer-process!
										:answer 	answer-process!
										:candidate 	candidate-process!)
	(webrtc-wrapper/create-offer! :pc               self-pc
	                      		  :success-callback offer-success!
	                          	  :failure-callback offer-failure! ))
