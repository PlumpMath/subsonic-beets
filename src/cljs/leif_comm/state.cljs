(ns leif-comm.state
  (:require [reagent.core :as reagent :refer [atom]]))


;;; -------------------------
;;; Application state

(defonce name-atom (atom "Anonymous"))
(defonce email-atom (atom ""))
(defonce password-atom (atom ""))
(defonce chat-log (atom {}))
