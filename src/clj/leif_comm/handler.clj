(ns leif-comm.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [leif-comm.middleware   :refer [wrap-middleware]]
            [leif-comm.templates    :refer [loading-page]]
            [leif-comm.broadcasting :refer [start-broadcaster! broadcast!]]
            [leif-comm.sente-routes     :as sente-routes]
            [config.core               :refer [env]]))


(defroutes routes
  ;; Web pages
  (GET  "/" [] loading-page)
  (GET  "/about" [] loading-page)
  (GET  "/register" [] loading-page)
  (GET  "/chat" [] loading-page)
  ;; Sente
  (GET  "/sente" req (sente-routes/ring-ajax-get-or-ws-handshake req))
  (POST "/sente" req (sente-routes/ring-ajax-post                req))
  (GET  "/broadcast" []
        (broadcast! 9001 sente-routes/connected-uids)
        (broadcast! 9001 :sente/all-users-without-uid))
  (GET  "/reset-sente-router" []
        sente-routes/start-router!)
  ;; Static content
  (resources "/")
  (not-found "Not Found"))


(def app (wrap-middleware #'routes))
