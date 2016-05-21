(ns webrtclojure.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary]
              [accountant.core :as accountant]
              [taoensso.sente  :as sente]
              [ajax.core :refer [GET POST]] ; Only for testing
              [webrtclojure.server-comms :as server-comms]
              [webrtclojure.webrtc :as webrtc]
              [webrtclojure.chat :as chat]))

;;; -------------------------
;;; Views
(defn atom-textarea-field [id value disabled]
  [:textarea {:id id
              :disabled disabled
              :value @value
              :on-change #(reset! value (-> % .-target .-value))}])

(defn atom-field [value placeholder]
[:input {:type "text"
         :value @value
         :placeholder placeholder
         :on-change #(reset! value (-> % .-target .-value))}])

(defonce name-atom (atom ""))
(defonce email-atom (atom ""))
(defonce password-atom (atom ""))
(defonce sendtextarea-atom (atom ""))

(defn home-page []
  [:div [:h2 "Welcome to this page!"]
   [:div [:a {:href "/about"} "About"]]
   [:div [:a {:href "/register"} "Register"]]
   [atom-field name-atom "Username"]
   [:input {:type "button" :value "Start" :on-click 
            #(do (server-comms/anonymous-login! @name-atom) 
              (secretary/dispatch! "/chat"))}]])

(defn about-page []
  (server-comms/channel-send! [::about])
  [:div [:h2 "About webrtclojure"]
   [:div [:a {:href "/"} "Go to the home page"]]])

(defn registry-page []
  [:div [:h2 "Welcome!"]
   [:h2 @server-comms/registry-result]
   [:h5 "How you want to be reached:"]
   [atom-field email-atom "Email"]
   [:h5 "Your secret passphrase:"]
   [atom-field password-atom "Password"]
   [:input {:type "button" :value "Register" :on-click
            #(server-comms/register! @email-atom @password-atom)}]])

(defn chat-page []
  (server-comms/channel-send! [::about])
  [:div {:id :chat} [:h2 "Chat room"]
<<<<<<< 1e5322bfb36467510b2cd718ce8e5d98685231ec
   [:div {:id :received} (atom-textarea-field :received chat/recvtextarea-atom true)]
   [:div {:id :send} (atom-textarea-field :send sendtextarea-atom false) 
            [:input {:id :send-btn :type "button" :value "Send" :on-click
              (fn [] (webrtc/dc-send-message! @sendtextarea-atom)
                (chat/append! @name-atom @sendtextarea-atom)
                (reset! sendtextarea-atom nil))}]]])
=======
   [:div {:id :received} [:textarea {:id :received :disabled true}]]
   [:div {:id :send} [:textarea {:id :send}] [:input {:id :send-btn :type "button" :value "Send" :on-click
            #(webrtc/dc-send-message! @name-atom)}]]])
>>>>>>> Added chat-page

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

(secretary/defroute "/chat" []
  (session/put! :current-page #'chat-page))

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
