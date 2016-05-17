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

(defn atom-field [value placeholder]
[:input {:type "text"
         :value @value
         :placeholder placeholder
         :on-change #(reset! value (-> % .-target .-value))}])

(defonce name-atom (atom ""))
(defonce email-atom (atom ""))
(defonce password-atom (atom ""))

(defn home-page []
  [:div [:h2 "Welcome to this page!"]
   [:div [:a {:href "/about"} "About"]]
   [:div [:a {:href "/register"} "Register"]]
   [atom-field name-atom "Username"]
   [:input {:type "button" :value "Start" :on-click
            #((GET "/restart-sente-router")
              (server-comms/anonymous-login @name-atom))}]
   [:input {:type "button" :value "Send" :on-click
            #(webrtc/dc-send-message! @name-atom)}]])

(defn about-page []
  (server-comms/channel-send! [::about])
  [:div [:h2 "About webrtclojure"]
   [:div [:a {:href "/"} "Go to the home page"]]])

(defn registry-page []
  [:div [:h2 "Welcome to this page!"]
   [:h5 "How you want to be reached:"]
   [atom-field email-atom "Email"]
   [:h5 "Your secret passphrase:"]
   [atom-field password-atom "Password"]
   [:input {:type "button" :value "Register" :on-click
            #(server-comms/register @email-atom @password-atom)}]])

(defn current-page []
  [:div [(session/get :current-page)]])

;;; -------------------------
;;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/register" []
  (session/put! :current-page #'registry-page))

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
