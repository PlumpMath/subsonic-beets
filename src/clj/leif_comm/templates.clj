(ns leif-comm.templates
  (:require [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))

(def mount-target
  [:div#app
   [:h3 "ClojureScript has not been compiled!"]
   [:p  "please run "
    [:b "lein figwheel"]
    " in order to start the compiler"]])

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
