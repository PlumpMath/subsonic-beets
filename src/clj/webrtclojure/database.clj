(ns webrtclojure.database
  (:require
   [korma.core :refer [defentity entity-fields table has-many]]
   [korma.db   :refer [defdb postgres default-connection]]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]
   [heroku-database-url-to-jdbc.core :as htj])
  (import org.postgresql.util.PSQLException))


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

;; Either use environment variable or default to current_user@localhost/webrtclojure.
;; You may need to trust all connections from 127.0.0.1 in /etc/postgresql/9.5/main/pg_hba.conf.
(def db-uri
  (java.net.URI. (or (System/getenv "DATABASE_URL")
                     (clojure.core/format "postgresql://%s@localhost:5432/webrtclojure"
                                          (System/getenv "USER")))))

;; Also sets this db as the default db for future korma calls.
(defdb db (korma-connection-map-from-uri db-uri))

;; For the ragtime migrations.
(defn- load-config []
  {:datastore  (jdbc/sql-database (.toString db-uri))
   :migrations (jdbc/load-resources "migrations")})


;;; --------------------
;;; Data types

(declare users tokens)

(defentity auth-tokens
  (table :authorization_tokens))

(defentity users
  (has-many auth-tokens)) ; Default fields on select


;;; --------------------
;;; Public functions

(defn migrate []
  (repl/migrate (load-config)))

(defn rollback []
  (repl/rollback (load-config)))

(defn safely "Wrap db-functions in a try-catch." [f]
  (try (f)
       (catch PSQLException e
         ((println (.getMessage e))
          ({:status 500
            :body   (str "Caught:" (.getMessage e))})))))
