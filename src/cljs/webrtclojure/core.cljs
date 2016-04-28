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

(defn username []
  [:div [:h3 "Enter nickname:"]])

(defn username-section [value]
[:input {:type "text"
         :value @value
         :on-change #(reset! value (-> % .-target .-value))}])

(defonce name-atom (reagent/atom "Abu"))

(defn shared-state []
    (fn []
      [:div
       [:h5 "What do you want ? "]
       [username-section name-atom]
       [:input {:type "button" :value "Start!" :on-click
                #(server-comms/anonymous-login "Bengt")}]
       [:p "We will call you " @name-atom "!"]]))

(defn what-is [what]
  [:h1 "WHAT IS " what "?"])

(defn home-page []
  [:div [:h2 "Welcome to this page!"]
   [:div [:a {:href "/about"} "go to about page"]]
   [shared-state]])

(defn about-page []
  (server-comms/channel-send! [::about])
  [:div [:h2 "About webrtclojure"]
   [:div [:a {:href "/"} "go to the home page"]]
   [what-is "THIS!"]])

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
