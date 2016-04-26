(ns webrtclojure.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary]
              [accountant.core :as accountant]
              [taoensso.sente  :as sente]
              [ajax.core :refer [GET POST]] ; Only for testing
              [webrtclojure.server-comms :as server-comms]
              [webrtclojure.webrtc :as webrtc]
              ))

;;; -------------------------
;;; Views

(defn home-page []
  [:div [:h2 "Welcome to webrtclojure"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
  (server-comms/channel-send! [::about])
  [:div [:h2 "About webrtclojure"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;;; -------------------------
;;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;;; -------------------------
;;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))

(defonce is-router-started? (server-comms/start-router!))


;;; -------------------------
;;; Signaling and data management

(defn onsignalingstatechange! [state]
    (.debug js/console "Signaling state change: %s" state))

(defn oniceconnectionstatechange! [state]
    (.debug js/console "Ice connection state change: %s" state))

(defn onicegatheringstatechange! [state]
    (.debug js/console "Ice gathering state change: %s" state))

;; Set up webrtc
(webrtc/create-data-connection!)

(GET "/restart-sente-router")
