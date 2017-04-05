(defproject viz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/dommy "1.1.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [reagent "0.6.1"]
                 [org.clojure/core.async "0.3.442"]]
  :plugins [[lein-figwheel "0.5.8"]]
  :source-paths ["src"]
  :clean-targets [:target-path "out"]
  :optimizations :none
  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :figwheel true
              :compiler {:main "viz.core"}
             }]
   })
