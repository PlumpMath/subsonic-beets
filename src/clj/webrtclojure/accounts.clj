(ns webrtclojure.accounts
  (:require [webrtclojure.database :as db]
            [korma.core :refer [insert values delete where select fields]]
            [buddy.core.nonce :as nonce]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]))

(defn get-salt "Get a salt to add to your user data." []
  (codecs/bytes->hex (nonce/random-nonce 16)))

(defn create "Create a user account for the given map." [user]
  (insert db/users
          (values
           (let [salt (get-salt)]
             (if (contains? user :password)
               (assoc user
                    :password (hashers/derive (str salt (:password user)))
                    :salt salt)
               user)))))

(defn correct-password?
  "Check whether the given password is correct for the given user id."
  [uid password]
  (let [result (first (select db/users
                       (fields :password :salt)
                       (where {:id uid})))]
    (hashers/check
     (str (:salt result) password)
     (:password result))))

(defn delete-user "Delete the account with the given user id." [uid]
  (delete db/users (where {:id uid})))

(defn delete-all "Delete all users." []
  (delete db/users))
