(ns webrtclojure.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [webrtclojure.middleware   :refer [wrap-middleware]]
            [webrtclojure.templates    :refer [loading-page]]
            [webrtclojure.broadcasting :refer [start-broadcaster! broadcast!]]
            [webrtclojure.sente-routes     :as sente-routes]
            [config.core               :refer [env]]))


(defroutes routes
  (GET  "/" [] loading-page)
  (GET  "/about" [] loading-page)
  (GET  "/sente" req (sente-routes/ring-ajax-get-or-ws-handshake req))
  (POST "/sente" req (sente-routes/ring-ajax-post                req))

  (GET  "/broadcast" []
        (broadcast! 9001 sente-routes/connected-uids)
        (broadcast! 9001 :sente/all-users-without-uid))

  (GET  "/reset-sente-router" []
        sente-routes/start-router!)

  (resources "/")
  (not-found "Not Found"))


(def app (wrap-middleware #'routes))
