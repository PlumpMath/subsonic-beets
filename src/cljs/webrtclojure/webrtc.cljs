(ns webrtclojure.webrtc
 (:require  [webrtclojure.webrtc-wrapper :as webrtc-wrapper]))

;;; ------------------------
;;; WebRTC connection handler

;; Global
(def ^:dynamic conncted-peers {}) 		;; Connected peers

;;; -------------------------
;;; Logger
(defn log-message 
	"Error message"
	[message]
	(.debug js/console (str "# " message)))

(defn log-message-fn [message] 
	 (fn [] (log-message message)))

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

	(webrtc-wrapper/add-peer-ice-candidate! :pc (:sender conncted-peers) 
											:candidate (webrtc-wrapper/create-ice-candidate! :candidate candidate) 
											:success-callback (log-message-fn "Candidate successfully added.")
											:failure-callback log-message-fn))

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
								  			   :onopen 		(log-message-fn "Data channel opened.")
								  			   :onclose 	(log-message-fn "Data channel closed.")
								  			   :onerror 	log-message-fn))
;;; -------------------------
;;; Answer handlers
(defn process-answer! 
	"Process recived answers"
	[send-fn sender answer]
	(log-message "Recived an answer, processing.")
	(let [session (webrtc-wrapper/create-session-description! nil)]  
		(aset session "type" (.-type answer))
		(aset session "sdp"  (.-sdp answer))
		(webrtc-wrapper/set-remote-description! :pc (:sender conncted-peers)
												:session-description session
												:success-callback (log-message-fn "Successfully added local description.")
												:failure-callback log-message-fn)))

(defn answer-success-fn
	"Callback function for newly requested answer"
	[send-fn sender pc]
	(fn [answer]
		(log-message "Answering offer.")
		(webrtc-wrapper/set-local-description! :pc 	pc 
											   :session-description answer
											   :success-callback (log-message-fn "Successfully added local description.")
											   :failure-callback log-message-fn)
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
											   :success-callback (log-message-fn "Successfully added local description.")
											   :failure-callback log-message-fn)
		(send-fn [:webrtclojure/offer { :receiver	sender
	              						:offer 	   (.stringify js/JSON offer)}] 8000)))

(defn session-success-fn 
	"Callback function for newly created sessions"
	[send-fn sender pc] 
	(fn []
		(log-message "New session created")
		(webrtc-wrapper/create-answer! :pc pc
								   	   :success-callback (answer-success-fn send-fn sender pc)
								   	   :failure-callback log-message-fn)))

(defn process-offer! 
	"Process newly recived offers"
	[send-fn sender offer]
	(log-message "Recived an offer, processing.")

	(let [pc (webrtc-wrapper/create-peer-connection!)
		  dc (webrtc-wrapper/create-data-channel! :pc pc)]
		(webrtc-wrapper/set-data-channel-callback! :dc 			dc
									  			   :onmessage  	dc-on-message!
									  			   :onopen 		(log-message-fn "Data channel opened.")
									  			   :onclose 	(log-message-fn "Data channel closed.")
									  			   :onerror 	log-message-fn)

		(webrtc-wrapper/set-peer-connection-callback! :pc pc
								  				  	  :onicecandidate (pc-on-ice-candidate-fn send-fn sender pc)
								  				  	  :ondatachannel  pc-on-data-channel!)
	
		(let [session (webrtc-wrapper/create-session-description! nil)]  
			(aset session "type" (.-type offer))
			(aset session "sdp"  (.-sdp offer))
			(webrtc-wrapper/set-remote-description! :pc pc
													:session-description session
													:success-callback 	 (session-success-fn send-fn sender pc)
													:failure-callback 	 log-message-fn))
		(set! conncted-peers (assoc conncted-peers :sender pc))))

;;; -------------------------
;;; New user handlers

(defn process-new-user! 
	"Process new users"
	[send-fn sender]
	(log-message "New user, processing.")
	(let [pc (webrtc-wrapper/create-peer-connection!)
		  dc (webrtc-wrapper/create-data-channel! :pc pc)]
		(webrtc-wrapper/set-data-channel-callback! 	:dc 			dc
								  			   		:onmessage  	dc-on-message!
								  			   		:onopen 		(log-message-fn "Data channel opened.")
								  			   		:onclose 		(log-message-fn "Data channel closed.")
								  			   		:onerror 		log-message-fn)
		  
		(webrtc-wrapper/set-peer-connection-callback! 	:pc pc
									  				  	:onicecandidate (pc-on-ice-candidate-fn send-fn sender pc)
									  				  	:ondatachannel  pc-on-data-channel!)

		(webrtc-wrapper/create-offer! 	:pc               pc
	                      		  		:success-callback (offer-success-fn send-fn sender pc)
	                          	  		:failure-callback log-message-fn)
		(set! conncted-peers (assoc conncted-peers :sender pc))))