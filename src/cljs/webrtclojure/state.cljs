(ns webrtclojure.state
  (:require [reagent.core :as reagent :refer [atom]]))


;;; -------------------------
;;; Application state

(defonce name-atom (atom ""))
(defonce email-atom (atom ""))
(defonce password-atom (atom ""))
(defonce recvtextarea-atom (atom ""))
(defonce sendtextarea-atom (atom ""))


;;; -------------------------
;;; Helper functions

(defn append! [user message]
  (swap! recvtextarea-atom str (str user ": " message "\n")))
