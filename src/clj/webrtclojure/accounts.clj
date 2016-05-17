(ns webrtclojure.accounts
  (:require [webrtclojure.database :as db]
            [korma.core :refer [insert values delete where select fields sql-only]]
            [buddy.core.nonce :as nonce]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]))

(defn get-salt "Get a salt to add to your user data." []
  (codecs/bytes->str (nonce/random-bytes 16)))

(defn salt-password [salt password]
  (str salt password))

(defn create-user "Create a user account for the given map." [user]
  (insert db/users
          (values
           (let [salt (get-salt)]
             (if (contains? user :password)
               (assoc user
                      :password (hashers/derive
                                 (salt-password salt (:password user)))
                      :salt salt)
               user)))))

(def create-anonymous-user (partial create-user {:salt "ORM:s suck"}))

(defn update-user "Update a user account." [user]
  (update db/users (values user)))

(defn correct-password?
  "Check whether the given password is correct for the given user id."
  [uid password]
  (let [result (first (select db/users
                       (fields :password :salt)
                       (where {:id uid})))]
    (hashers/check
     (salt-password (:salt result) password)
     (:password result))))

(defn delete-user "Delete the account with the given user id." [uid]
  (delete db/users (where {:id uid})))

(defn delete-all "Clean out the database." []
  (delete db/users)
  (delete db/auth-tokens))
