(ns leif-comm.pages
  (:require [reagent.core :as reagent :refer [atom]]
            [accountant.core :as accountant]
            [leif-comm.state :as state]
            [leif-comm.webrtc :as webrtc]
            [leif-comm.server-comms :as server-comms]))


(defn atom-field [atom placeholder type]
[:input {:type type
         :value @atom
         :placeholder placeholder
         :on-change #(reset! atom (-> % .-target .-value))}])

(defn atom-textarea [id atom disabled]
  [:textarea {:id id
              :disabled disabled
              :value @atom
              :on-change #(reset! atom (-> % .-target .-value))}])


(defn home []
  [:div [:h2 "Welcome to LEIF-comm!"]
   [:div [:a {:href "/about"} "About"]]
   [:div [:a {:href "/register"} "Register"]]
   [atom-field state/name-atom "Nickname" "text"]
   [:input {:type "button" :value "Start" :on-click
            #(do (server-comms/anonymous-login! @state/name-atom)
              (accountant/navigate! "/chat"))}]])

(defn chat []
  [:div {:id :chat}
   [:h2 "Chat room"]
   [:a {:href "/"} "Back"][:br]
   [atom-field state/name-atom "Nickname" "text"]
   [atom-textarea :received state/recvtextarea-atom true]
   [atom-textarea :send state/sendtextarea-atom false]
   [:input {:id :send-btn :type "button" :value "Send" :on-click
            #(do (server-comms/send-message! @state/sendtextarea-atom)
                 (state/append! state/recvtextarea-atom @state/name-atom @state/sendtextarea-atom)
                 (reset! state/sendtextarea-atom ""))}]])
