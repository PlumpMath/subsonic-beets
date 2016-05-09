(ns webrtclojure.database
  (:require
   [korma.core :refer :all]
   [korma.db   :refer [defdb postgres default-connection]]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]
   [heroku-database-url-to-jdbc.core :as htj]))


;;; --------------------
;;; Helper functions

(defn- parse-username-and-password [db-uri]
  (if (nil? (.getUserInfo db-uri))
    nil (clojure.string/split (.getUserInfo db-uri) #":")))

(defn- subname [db-uri]
  (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))

(defn- korma-connection-map-from-uri
  "Converts a java.net.URI to a korma defdb map."
  [db-uri]
  (let [[username password] (parse-username-and-password db-uri)]
    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user username
     :password password
     :subname (subname db-uri)}))

(defn- korma-connection-map-from-url
  "Converts Heroku's DATABASE_URL to a map that you can pass to Korma's
  defdb function."
  [db-url]
  (korma-connection-map-from-uri (java.net.URI. db-url)))



;;; --------------------
;;; Setup

;; Either use environment variable or default to localhost.
(def db-uri (java.net.URI. (or (System/getenv "DATABASE_URL")
                               "postgresql://localhost:5432/:webrtclojure")))

;; Also sets this db as the default db for future korma calls.
(defdb db (korma-connection-map-from-uri db-uri))

;; For the ragtime migrations.
(defn- load-config []
  {:datastore  (jdbc/sql-database db-uri)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate (load-config)))

(defn rollback []
  (repl/rollback (load-config)))



;;; --------------------
;;; Data types

(declare users tokens)

(defentity auth-tokens
  (table :authorization_tokens)
  (entity-fields :uid :token))

(defentity users
  (has-many auth-tokens)
  (entity-fields :uid :username :email)) ; Default fields on select
