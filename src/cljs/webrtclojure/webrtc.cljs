(ns webrtclojure.webrtc
 (:require  [webrtclojure.webrtc-wrapper :as webrtc-wrapper]
 			[webrtclojure.server-comms   :as server-comms])
	)

;;; ------------------------
;;; WebRTC connection handler

;; Global
(def ^:dynamic self-pc nil) 			;; Our local peer connection
(def ^:dynamic conncted-peers []) 		;; Connected peers

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
	                            {:sdp (.stringify js/JSON sdp)}]
	                            8000))  ;; timeout
(defn answer-failure! 
  	"Callback function when the creation of an answer fails"
	[sdp]
    (.debug js/console "# Failed to create an answer")) 


;;; -------------------------
;;; Offer handlers

(defn offer-process! 
	"Process recived offers"
	[sdp]
	(.debug js/console "# Recived an offer, processing...")

	;; Createa new peer connection for the remote peer
	(def pc (webrtc-wrapper/create-peer-connection!))

	;; Create a data channel
	(def dc (webrtc-wrapper/create-data-channel! :pc pc))

	;; Set the remote description 
	(webrtc-wrapper/set-remote-description! :pc pc
	:session-description (webrtc-wrapper/create-session-description! sdp))
	
	;; Create an answer
	(webrtc-wrapper/create-answer! :pc pc
								   :success-callback answer-success!
								   :failure-callback answer-failure!)

	;; Store the peer connection
	(set! conncted-peers (conj conncted-peers {pc dc})))

(defn offer-success! 
	"Callback function to trigger offer singaling, with the created offer"
	[sdp]
	(.setLocalDescription self-pc sdp)

	;; Signal
	;; TODO: Find a more suitable solution for stringify
	(.debug js/console "# Signaling offer...")
	(server-comms/channel-send! [:webrtclojure/offer
	                            {:sdp (.stringify js/JSON sdp)}]
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
	(server-comms/set-message-handlers! :broadcast  broadcast-process!
										:offer 		offer-process!
										:answer 	answer-process!)
	(webrtc-wrapper/create-offer! :pc               self-pc
	                      :success-callback offer-success!
	                      :failure-callback offer-failure! ))
