(ns subsonic-beets.handler
  (:require [compojure.core            :refer [GET POST defroutes]]
            [compojure.route           :refer [not-found resources]]
            [subsonic-beets.middleware   :refer [wrap-middleware]]
            [subsonic-beets.templates    :refer [loading-page]]
            [subsonic-beets.broadcasting :refer [start-broadcaster! broadcast!]]
            [subsonic-beets.sente-routes     :as sente-routes]
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
        sente-routes/restart-router!)
  ;; Static content
  (resources "/")
  (not-found "Not Found"))


(def app (wrap-middleware #'routes))
