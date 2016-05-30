(ns webrtclojure.pages
  (:require [reagent.core :as reagent :refer [atom]]
            [accountant.core :as accountant]
            [webrtclojure.state :as state]
            [webrtclojure.webrtc :as webrtc]
            [webrtclojure.server-comms :as server-comms]))


(defn atom-field [value placeholder]
[:input {:type "text"
         :value @value
         :placeholder placeholder
         :on-change #(reset! value (-> % .-target .-value))}])

(defn atom-textarea [id value disabled]
  [:textarea {:id id
              :disabled disabled
              :value @value
              :on-change #(reset! value (-> % .-target .-value))}])


(defn home []
  [:div [:h2 "Welcome to this page!"]
   [:div [:a {:href "/about"} "About"]]
   [:div [:a {:href "/register"} "Register"]]
   [atom-field state/name-atom "Username"]
   [:input {:type "button" :value "Start" :on-click
            #(do (server-comms/anonymous-login! @state/name-atom)
              (accountant/navigate! "/chat"))}]])

(defn about []
  [:div [:h2 "About webrtclojure"]
   [:div [:a {:href "/"} "Go to the home page"]]])

(defn registry []
  [:div [:h2 "Welcome!"]
   [:h2 @server-comms/registry-result]
   [:h5 "How you want to be reached:"]
   [atom-field state/email-atom "Email"]
   [:h5 "Your secret passphrase:"]
   [atom-field state/password-atom "Password"]
   [:input {:type "button" :value "Register" :on-click
            #(server-comms/register! @state/email-atom @state/password-atom)}]])

(defn chat []
  [:div {:id :chat} [:h2 "Chat room"]
   [:div {:id :received}
    (atom-textarea :received state/recvtextarea-atom true)]
   [:div {:id :send}
     (atom-textarea :send state/sendtextarea-atom false)
    [:input {:id :send-btn :type "button" :value "Send" :on-click
             #(do (webrtc/dc-send-message! @state/sendtextarea-atom)
                  (state/append! @state/name-atom @state/sendtextarea-atom)
                  (reset! state/sendtextarea-atom ""))}]]
   ])
