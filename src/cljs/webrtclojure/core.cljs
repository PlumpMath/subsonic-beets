(ns webrtclojure.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary]
              [accountant.core :as accountant]
              [taoensso.sente  :as sente]
              [ajax.core :refer [GET POST]] ; Only for testing
              [webrtclojure.server-comms :as server-comms]
              ))

;;; -------------------------
;;; Views

(defn home-page []
  [:div [:h2 "Welcome to webrtclojure"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
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

(GET "/broadcast") ; Trigger a sente broadcast
