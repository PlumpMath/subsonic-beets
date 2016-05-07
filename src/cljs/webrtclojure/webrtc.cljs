(ns webrtclojure.webrtc
 (:require  [webrtclojure.webrtc-wrapper :as webrtc-wrapper]))

;;; ------------------------
;;; WebRTC connection handler

;; Global
(def ^:dynamic self-pc nil) 			;; Our local peer connection
(def ^:dynamic self-dc nil) 			;; Our local data channel
(def ^:dynamic conncted-peers []) 		;; Connected peers

;;; -------------------------
;;; Logger
(defn log-error 
	"Error logger"
	[message]
	(.debug js/console message))

(defn log-message [message] 
	 (fn []  (log-error (str "# " message))))

;;; -------------------------
;;; Data channel handlers

(defn dc-on-message! [message] 
	 (.debug js/console "## Recived message on data channel: " (aget message "data")))

;;; -------------------------
;;; Connection peer handlers
(defn process-candidate! 
	"Process recived candidate"
	[send-fn sender candidate]
	(.debug js/console "# Recived an candidate, processing.")

	(webrtc-wrapper/add-peer-ice-candidate! :pc self-pc 
	:candidate (webrtc-wrapper/create-ice-candidate! :candidate candidate) 
	:success-callback (log-message "Candidate successfully added.")
	:failure-callback log-error))

(defn pc-on-ice-candidate-fn 
	[send-fn sender pc] 
	(fn [event] 
	 (log-message "New ICE candida.")
	 (if (and (not(nil? event)) (not(nil? (aget event "candidate")))) 
	 	(send-fn [:webrtclojure/candidate
				  { :receiver 	sender
				  	:candidate (.stringify js/JSON (aget event "candidate"))}] 8000))))

(defn pc-on-data-channel! [event] 
	(webrtc-wrapper/set-data-channel-callback! :dc 			(aget event "channel")
								  			   :onmessage  	dc-on-message!
								  			   :onopen 		(log-message "Data channel opened.")
								  			   :onclose 	(log-message "Data channel closed.")
								  			   :onerror 	log-error))
;;; -------------------------
;;; Answer handlers
(defn process-answer! 
	"Process recived answers"
	[send-fn sender answer]
	(log-message "Recived an answer, processing.")
	(def answer-session (webrtc-wrapper/create-session-description! answer)) 
	(aset answer-session "type" (.-type answer))
	(aset answer-session "sdp"  (.-sdp answer))
	(webrtc-wrapper/set-remote-description! :pc self-pc
											:session-description answer-session
											:success-callback (log-message "Successfully added local description.")
											:failure-callback log-error))

(defn answer-success-fn
	"Callback function for newly requested answer"
	[send-fn sender pc]
	(fn [answer]
		(log-message "Answering offer.")
		(webrtc-wrapper/set-local-description! :pc 	pc 
											   :session-description answer
											   :success-callback (log-message "Successfully added local description.")
											   :failure-callback log-error)
		(send-fn [:webrtclojure/answer  { :receiver	sender
	              						  :answer 	(.stringify js/JSON answer)}] 8000)))


;;; -------------------------
;;; Offer handlers
(defn offer-success-fn 
	"Callback function for newly requested offer"
	[send-fn sender pc] 
	(fn [offer]
		(log-message "Sending requested offer.")
		(webrtc-wrapper/set-local-description! :pc 	pc 
											   :session-description offer
											   :success-callback (log-message "Successfully added local description.")
											   :failure-callback log-error)
		(send-fn [:webrtclojure/offer { :receiver	sender
	              						:offer 	   (.stringify js/JSON offer)}] 8000)))

(defn session-success-fn 
	"Callback function for newly created sessions"
	[send-fn sender pc] 
	(fn []
		(log-message "New session created")
		(webrtc-wrapper/create-answer! :pc pc
								   	   :success-callback (answer-success-fn send-fn sender pc)
								   	   :failure-callback log-error)))

(defn process-offer! 
	"Process newly recived offers"
	[send-fn sender offer]
	(log-message "Recived an offer, processing.")

	(set! self-pc (webrtc-wrapper/create-peer-connection!))
	(set! self-dc (webrtc-wrapper/create-data-channel! :pc self-pc))
	(webrtc-wrapper/set-data-channel-callback! :dc 			self-dc
								  			   :onmessage  	dc-on-message!
								  			   :onopen 		(log-message "Data channel opened.")
								  			   :onclose 	(log-message "Data channel closed.")
								  			   :onerror 	log-error)

	(webrtc-wrapper/set-peer-connection-callback! :pc self-pc
								  				  :onicecandidate (pc-on-ice-candidate-fn send-fn sender self-pc)
								  				  :ondatachannel  pc-on-data-channel!)
	
	(def session (webrtc-wrapper/create-session-description! nil)) 
	(aset session "type" (.-type offer))
	(aset session "sdp"  (.-sdp offer))

	(webrtc-wrapper/set-remote-description! :pc self-pc
											:session-description session
											:success-callback 	 (session-success-fn send-fn sender self-pc)
											:failure-callback 	 log-error)
	
	;; Store the peer connection
	;; TODO: This will be used later, we will also need to 
	;; 		 store uuid of the users, for singaling.
	;;(set! conncted-peers (conj conncted-peers {pc dc}))
	)

;;; -------------------------
;;; New user handlers

(defn process-new-user! 
	"Process new users"
	[send-fn sender]
	(log-message "New user, processing.")
	(set! self-pc (webrtc-wrapper/create-peer-connection!))
	(set! self-dc (webrtc-wrapper/create-data-channel! :pc self-pc))
	(webrtc-wrapper/set-data-channel-callback! :dc 			self-dc
								  			   :onmessage  	dc-on-message!
								  			   :onopen 		(log-message "Data channel opened.")
								  			   :onclose 	(log-message "Data channel closed.")
								  			   :onerror 	log-error)

	(webrtc-wrapper/set-peer-connection-callback! :pc self-pc
								  				  :onicecandidate (pc-on-ice-candidate-fn send-fn sender self-pc)
								  				  :ondatachannel  pc-on-data-channel!)


	(webrtc-wrapper/create-offer! :pc               self-pc
	                      		  :success-callback (offer-success-fn send-fn sender self-pc)
	                          	  :failure-callback log-error))