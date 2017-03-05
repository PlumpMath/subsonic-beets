(defproject subsonic-beets "0.1.0"
  :description "A subsonic server implementation feeding off of data from a beets db."
  :url "https://github.com/Rovanion/subsonic-beets"
  :license {:name "AGPLv3"
            :url  "http://www.gnu.org/licenses/agpl-3.0.html"}

  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [org.clojure/tools.reader "0.10.0"] ; Dependency issue between sente and ring.
   [org.clojure/core.cache "0.6.4"]    ; Version requiered by figwheel.

   [ring-server "0.4.0"]               ; Library for starting a web server.
   [ring/ring-core "1.5.1"]            ; HTTP abstraction library.
   [ring/ring-defaults "0.2.1"]        ; Middleware collection.
   [yogthos/config "0.8"]              ; Managing environment configs.
   [compojure "1.5.2"]                 ; Routing.
   [http-kit "2.2.0"]                 ; Our web server.

   [org.clojure/clojurescript "1.9.293"]
   [reagent "0.6.0"                    ; React abstraction.
    :exclusions [org.clojure/tools.reader]]
   [reagent-forms "0.5.22"]
   [reagent-utils "0.1.7"]
   [secretary "1.2.3"]                 ; Client side routing.
   [venantius/accountant "0.1.7"]      ; Managing the URL bar in the browser.
   [com.taoensso/sente "1.11.0"]]      ; WebSockets manager.

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.1"]
            [lein-asset-minifier "0.2.7"
             :exclusions [org.clojure/clojure]]]

  :ring {:handler      subsonic-beets.handler/app
         :uberwar-name "subsonic-beets.war"}

  :min-lein-version "2.7.1"

  :uberjar-name "subsonic-beets.jar"

  :main subsonic-beets.server

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths   ["src/clj"   "src/cljc"]
  :test-paths     ["test/clj"  "test/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :figwheel {:http-server-root "public"
             :server-port      3449
             :nrepl-port       7002
             :css-dirs         ["resources/public/css"]
             :ring-handler     subsonic-beets.handler/app
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "cemerick.piggieback/wrap-cljs-repl"]}

  :cljsbuild {:builds
              {:dev
               {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                :figwheel     {:on-jsload "subsonic-beets.core/on-js-reload"
                               :open-urls ["http://localhost:3449/chat"]}
                :compiler     {:main                 subsonic-beets.dev
                               :output-to            "target/cljsbuild/public/js/app.js"
                               :output-dir           "target/cljsbuild/public/js/out"
                               :asset-path           "/js/out"
                               :source-map-timestamp true
                               :optimizations        :none
                               :pretty-print         true
                               :preloads             [devtools.preload]}}
               :min
               {:source-paths ["src/cljs" "src/cljc"]
                :compiler     {:output-to     "target/cljsbuild/public/js/app.js"
                               :main          subsonic-beets.core
                               :optimizations :advanced
                               :pretty-print  false}}}}

  :profiles {:dev
             {:dependencies [[ring/ring-mock              "0.3.0"]
                             [ring/ring-devel             "1.4.0"]
                             [prone                       "1.1.1"]
                             [lein-figwheel               "0.5.8"]
                             [org.clojure/tools.nrepl     "0.2.12"]
                             [com.cemerick/piggieback     "0.2.1"]
                             [figwheel-sidecar            "0.5.8"]
                             [cider/cider-nrepl           "0.14.0"]
                             [binaryage/devtools          "0.8.3"]]
              :env          {:dev true}
              :source-paths ["env/dev/clj"]
              :plugins      [[lein-figwheel "0.5.8"]]
              :repl-options {:init             (set! *print-length* 50)
                             :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
              :cljsbuild    {:builds {:dev
                                      {:source-paths ["env/dev/cljs"]
                                       :compiler
                                       {:main          "subsonic-beets.dev"
                                        :pretty-print  false}}}}}}
             :uberjar
             {:hooks        [minify-assets.plugin/hooks]
              :source-paths ["env/prod/clj"]
              :prep-tasks   ["compile" ["cljsbuild" "once"]]
              :env          {:production true}
              :aot          :all
              :omit-source  true
              :cljsbuild    {:jar    true
                             :builds {:min
                                      {:source-paths ["env/prod/cljs"]
                                       :compiler
                                       {:main          "subsonic-beets.prod"
                                        :optimizations :advanced
                                        :pretty-print  false}}}}}})
