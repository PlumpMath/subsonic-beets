(ns webrtclojure.accounts
  (:require [webrtclojure.database :as db :refer [safely]]
            [korma.core :refer :all :rename {update sql-update}]
            [buddy.core.nonce :as nonce]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [webrtclojure.util :as util]))


;;; --------------------
;;; Helper functions

(defn- get-salt "Get a salt to add to your user data." []
  (str (nonce/random-bytes 16)))

(defn- salt-password [salt password]
  (str salt password))

(defn- salt-n-pepper
  "Make a user ready for input to the database"
  [user]
  (if (contains? user :password)
    (let [salt (get-salt)]
      (assoc user
             :password (hashers/derive
                        (salt-password salt (:password user)))
             :salt salt))
    user))


;;; --------------------
;;; Actions

(defn create-user! "Create a user account for the given map." [user]
  (safely
   #(insert db/users
           (values (salt-n-pepper user)))))

;; There must be at least one field in the map sent to KORMA, else it fails.
(def create-anonymous-user! (partial create-user! {:salt "ORM:s suck"}))

(defn update-user! "Update a user account." [uid user]
  (safely #(sql-update db/users
                       (set-fields (salt-n-pepper user))
                       (where {:id uid}))))

(defn correct-password?
  "Check whether the given password is correct for the given user id."
  [uid password]
  (let [result (first (safely #(select db/users
                                       (fields :password :salt)
                                       (where {:id uid}))))]
    (hashers/check
     (salt-password (:salt result) password)
     (:password result))))

(defn delete-user! "Delete the account with the given user id." [uid]
  (safely #(delete db/users (where {:id uid}))))

(defn delete-all! "Clean out the entire database." []
  (safely #(delete db/users))
  (safely #(delete db/auth-tokens)))
