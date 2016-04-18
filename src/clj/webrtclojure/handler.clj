(ns webrtclojure.handler
  (:require [compojure.core          :refer [GET POST defroutes]]
            [compojure.route         :refer [not-found resources]]
            [webrtclojure.middleware :refer [wrap-middleware]]
            [webrtclojure.templates  :refer [loading-page]]
            [config.core             :refer [env]]
            [taoensso.sente          :as sente]
            [taoensso.sente.server-adapters.http-kit
                                     :refer (sente-web-server-adapter)]))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)
  (GET  "/sente" req (ring-ajax-get-or-ws-handshake req))
  (POST "/sente" req (ring-ajax-post                req))

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))


;; (def my-app
;;   (-> my-app-routes
;;       ;; Add necessary Ring middleware:
;;       ring.middleware.keyword-params/wrap-keyword-params
;;       ring.middleware.params/wrap-params))
