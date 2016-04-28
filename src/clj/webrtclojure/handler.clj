(ns webrtclojure.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [webrtclojure.middleware   :refer [wrap-middleware]]
            [webrtclojure.templates    :refer [loading-page]]
            [webrtclojure.broadcasting :refer [start-broadcaster! broadcast!]]
            [config.core               :refer [env]]
            [taoensso.sente            :as sente]
            [taoensso.sente.server-adapters.http-kit
                                       :refer (sente-web-server-adapter)]))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def receive-channel               ch-recv) ; ChannelSocket's receive channel
  (def channel-send!                 send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

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
    (println "Channel socket successfully established!")
    (println "Channel socket state change: %s" ?data)))

(defmethod -message-handler :chsk/handshake
  ;; Handshake for WS
  [{:keys [?data]}]
  (let [[?uid] ?data]
    (println "Handshake done for: %s" ?uid)))

;;; Application specific routes
(defmethod -message-handler :webrtclojure/signal
  [{:as ev-msg :keys [?data]}]
  (println "Server received a signal: %s" :event)
  (broadcast! :webrtclojure/offer (get-in (:event ev-msg) [1] )   ;; get data from event
                                  connected-uids 
                                  channel-send!))

(defmethod -message-handler :webrtclojure/answer
  [{:as ev-msg :keys [?data]}]
  (println "Server received a answer: %s" :event)
  (broadcast! :webrtclojure/offer (get-in (:event ev-msg) [1] )   ;; get data from event
                                  connected-uids 
                                  channel-send!))
;;; -------------------------
;;; Router lifecycle.

(defonce router (atom nil))
;; Stop router if it exists.
(defn stop-router! [] (when-let [stop-f @router] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router
          (sente/start-chsk-router! receive-channel message-handler)))

(defonce is-router-started? (start-router!))


(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET  "/sente" req (ring-ajax-get-or-ws-handshake req))
  (POST "/sente" req (ring-ajax-post                req))

  (GET "/broadcast" [] (broadcast! 9001 connected-uids channel-send!)(broadcast! 9001 :sente/all-users-without-uid channel-send!))
  (GET "/reset" [] (start-router!))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))

