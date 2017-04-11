(ns viz.running-actor-menu
  (:require 
   [cljsjs.material-ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.icons :as ic]
   [cljs.core.async :refer [put! chan <!]]
   ))

(defn running-actor-menu [event-channel state]
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                {:palette {:text-color (color :green600)}})}
   [ui/paper {:style (clj->js
                      {:float "left" 
                       :display (if (= true (get-in state [:open])) "inline-block" "none")
                       :position "absolute" 
                       :top (get-in state [:y]) 
                       :left (get-in state [:x])})
              }
    [ui/menu {:auto-width false}
     [ui/menu-item {
                    :primary-text "Send Message" 
                    :right-icon (ic/communication-message)
                    :on-touch-tap #(put! event-channel [:open-message-modal (get-in state [:pid])])
                    }]
     [ui/menu-item {:primary-text "View State"}]
     [ui/menu-item {:primary-text "View Message Log" :right-icon (ic/action-list)}]
]]])
