(defproject viz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.3.442"]
                 [reagent "0.6.1" :exclusions [org.clojure/tools.reader cljsjs/react cljsjs/react-dom]]
                 [cljsjs/codemirror "5.24.0-1"]
                 ;; [cljsjs/material-ui "0.17.1-0"]
                 [cljs-react-material-ui "0.2.39"]
                 [cljsjs/phoenix "1.2.0-0"]
                 [cljsjs/jquery "2.2.4-0"]
                 [cljsjs/pixi "4.4.3-0"]
                 ;; [cljsjs/ace "1.2.6-0"]
                 ]
  :plugins [[lein-figwheel "0.5.8"]
            [refactor-nrepl "2.2.0"]
            [lein-cljsbuild "1.1.5"]
            [cider/cider-nrepl "0.13.0"]]
  :source-paths ["src"]
  ;; :main viz.core
  :clean-targets [:target-path "out"]
  :optimizations :none
  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :figwheel true
              :compiler {:main "viz.core"
                         :asset-path "js/out"
                         :output-to "resources/public/js/main.js"
                         :output-dir "resources/public/js/out"
                         ;; :optimizations :advanced
                         :source-map-timestamp true}
             }]
              }
  :figwheel {
             :open-file-command "emacsclient"
             :nrepl-port 7888
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"]
             })
