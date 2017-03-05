(ns subsonic-beets.templates
  (:require [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
   [:h5 {:id "loading-message"} "Connecting..."]])

(def loading-page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name    "viewport"
            :content "width=device-width, initial-scale=1"}]
    (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))]
   [:body
    mount-target
    (include-js "/js/app.js")]))
