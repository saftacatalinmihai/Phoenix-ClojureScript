(defproject viz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/dommy "1.1.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [reagent "0.6.1"]
                 [org.clojure/core.async "0.3.442"]]
  :plugins [[lein-figwheel "0.5.8"]
            [refactor-nrepl "2.2.0"]
            [cider/cider-nrepl "0.13.0"]]
  :source-paths ["src"]
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
                         :source-map-timestamp true}
             }]
              }
  :figwheel {
             :open-file-command "emacsclient"
             :nrepl-port 7888
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"]
             })
