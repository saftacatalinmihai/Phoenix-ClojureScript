(ns viz.menues
  (:require
   [cljsjs.material-ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.icons :as ic]
   [cljs.core.async :refer [put! chan <!]]
   ))

(defn no-menu-opened? [state] (not (get-in @state [:some-menu-opened :open])))
(defn some-menu-opened? [state] (get-in @state [:some-menu-opened :open]))
(defn open-component-menu [state component-menu x y]
  (js/console.log (pr-str  "open component" component-menu))
  (js/console.log (pr-str (get @state component-menu)))
  (swap! (get @state component-menu) #(-> % (assoc :x x :y y :open true)))
  (swap! state assoc-in [:some-menu-opened] {:open true :x x :y y})
)
(defn close-other-menues [state x y]
  (js/console.log "close others")
  (swap! (:main-menu @state) assoc-in [:open] false)
  (swap! (:running-actor-menu @state) assoc-in [:open] false)
  (swap! state assoc-in [:some-menu-opened] {:open false :x x :y y})
)
(defn menues-eq-xy [m1 m2]
  (and (= (:x m1) (:x m2))) (= (:y m1) (:y m2)))


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
     [ui/menu-item {:primary-text "New Actor Type" :right-icon (ic/social-person-outline)
                    :on-touch-tap #(do
                                     (put! event-channel [:open-new-actor-input :ok])
                                     (swap! state assoc :open false))}]
]]])
