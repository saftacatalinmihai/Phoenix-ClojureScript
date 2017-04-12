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
                    :on-touch-tap #(do
                                     (put! event-channel [:open-message-input (get @state :pid)])
                                     (swap! state assoc :open false)
                                     )
                    }]
     [ui/menu-item {:primary-text "View State" :right-icon (ic/hardware-memory)}]
     [ui/menu-item {:primary-text "View Message Log" :right-icon (ic/action-list)}]
]]])

(defn main-menu [event-channel state]
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                {:palette {:text-color (color :green600)}})}
   [ui/paper {:style (clj->js
                      {:position "absolute"
                       :float "left"
                       :display (if (= (@state :open) true) "inline-block" "none")
                       :top (get @state :y)
                       :left (get @state :x)})}
    [ui/menu {:auto-width false}
     [ui/menu-item {:primary-text "New message" :right-icon (ic/communication-message)}]
     [ui/menu-item {:primary-text "New Actor Type" :right-icon (ic/social-person-outline)}]
]]])
