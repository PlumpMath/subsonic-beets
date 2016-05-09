(ns webrtclojure.accounts
  (:require [webrtclojure.database :as db]
            [korma.core :refer [insert values]]))

(defn create "Create a user account." [user]
  (println "Creating user:" user)
  "Add a user to the users table."
  (insert db/users (values user)))

(defn correct-password?
  "Check whether the given password is correct for the given user."
  [uid password])
