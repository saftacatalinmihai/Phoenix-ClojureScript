(ns viz.menues
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
                       :display (if (= true (get @state :open)) "inline-block" "none")
                       :position "absolute" 
                       :top (get @state :y)
                       :left (get @state :x)})
              }
    [ui/menu {:auto-width false}
     [ui/menu-item {
                    :primary-text "Send Message" 
                    :right-icon (ic/communication-message)
                    :on-touch-tap #( put! event-channel [:open-message-modal (get @state :pid)])
                    }]
     [ui/menu-item {:primary-text "View State"}]
     [ui/menu-item {:primary-text "View Message Log" :right-icon (ic/action-list)}]
]]])

(defn main-menu [event-channel state]
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                {:palette {:text-color (color :green600)}})}
   [ui/paper {:style (clj->js 
                      {:position "absolute" 
                       :float "left"
                       :display (if (= true (get @state :open)) "inline-block" "none")
                       :top (get @state :y) 
                       :left (get @state :x)})}
    [ui/menu {:auto-width false}
     [ui/menu-item {:primary-text "5" :right-icon (ic/action-favorite)}]
     [ui/menu-item {:primary-text "6" :right-icon (ic/social-group)}]
     [ui/menu-item {:primary-text "7"}]
     [ui/menu-item {:primary-text "8"}]
]]])
