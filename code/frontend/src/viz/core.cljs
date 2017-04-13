(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljsjs.material-ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.icons :as ic]
   [reagent.core :as r]
   [viz.channel :as channel]
   [viz.graphics :as graphics]
   [viz.menues :as m]
   [cljs.core.async :refer [put! chan <!]]))

;; Core event channel
(def core-chan (chan))
(defn raise-event [event-name event-data]
  (put! core-chan [event-name event-data]))

;; REAGENT STATE
(defonce state (r/atom {
                        :message-input-dialog (r/atom {:id :message-input-dialog
                                                       :title "Message"
                                                       :open false
                                                       :action (fn [st value]
                                                                 (put! core-chan [:send-actor-message value])
                                                                 (swap! st assoc :open false))
                                                       })
                        :add-actor-input-dialog (r/atom {:id :message-input-dialog
                                                         :title "Add actor"
                                                         :open false
                                                         :action (fn [st value]
                                                                   (put! core-chan [:new_actor_type value])
                                                                   (swap! st assoc :open false))
                                                         })
                        :some-menu-opened (r/atom {:open false :x 0 :y 0})
                        :main-menu (r/atom {:x 0 :y 0 :open false})
                        :running-actor-menu (r/atom {:x 5 :y 5 :open false})
                        :editor (r/atom {:actor-type "" :open false})
                        :response (r/atom {:header "Response" :value "" :open false :color (color :green600)})
                        :error (r/atom {:header "Error" :value "" :open false :color (color :red500)})
                        :send_message (r/atom {:to "" :msg ""})
                        :new-actor (r/atom {:type ""})
                        :snackbar (r/atom {:message "" :open false})
                        }))

;; Reagent components
(defn pixi []
  [:div {:id "pixi-js"}])

(defn slide-out-editor [event-channel state]
  [ui/mui-theme-provider
   [ui/drawer {
               :open (@state :open)
               :open-secondary true
               :width (/ (-> js/window js/jQuery .width) 2)
               :docked false
               :on-request-change #(if (= % false) (swap! state assoc :open false))
               }
    [:div {:id "editor"}]
    ]]
  )

(defn on-enter [e f]
  (if (= 13 (.-charCode e)) (f)))

(defn input-dialog [event-channel state]
  (let [value (atom "")]
    [ui/mui-theme-provider {get-mui-theme {:palette {:text-color (color :green600)}}}
     [:div
      [ui/dialog {:title (:title @state)
                  :actions [(r/as-element
                             [ui/flat-button
                              {:label "Submit"
                               :primary true
                               :on-touch-tap #((@state :action) state @value)}])]
                  :open (:open @state)
                  :on-request-close #(swap! state assoc :open false)
                  }
       [ui/text-field {
                       :id "input-dialog"
                       :hint-text ""
                       :default-value @value
                       :on-change #(reset! value %2)
                       :on-key-press (fn [e] (on-enter e #((@state :action) state @value)))
                       }]]]]))

(defn bottom-resp [state]
  [ui/mui-theme-provider {get-mui-theme {:palette {:text-color (color :green600)}
                                         }}
   [ui/paper {:style (clj->js
                      {:display (if (= true (get @state :open)) "inline-block" "none")
                       :position "absolute"
                       :bottom "0px"
                       :width "100%"})
              }
    [ui/text-field {:id "response"
                    :full-width true
                    :value (@state :value)
                    :underline-style {:border-color (@state :color)}
                    }]]
   ]
)

(defn snack-bar [state]
[ui/mui-theme-provider
 [ui/snackbar {:open (@state :open)
               :message (@state :message)
               :auto-hide-duration 4000
               :content-style {:color (get @state :color (color :white500))}
               :on-request-close #(swap! state assoc :open false)
               }]])

(defn reagent-mount []
  [:div
   [m/main-menu core-chan (:main-menu @state)]
   [m/running-actor-menu core-chan (:running-actor-menu @state)]
   [input-dialog core-chan (:message-input-dialog @state)]
   [input-dialog core-chan (:add-actor-input-dialog @state)]
   [slide-out-editor core-chan (@state :editor)]
   [bottom-resp (@state :response)]
   [bottom-resp (@state :error)]
   [snack-bar (@state :snackbar)]
   ])

(r/render [reagent-mount]
          (js/document.querySelector "#reagent-mount"))

(r/render [pixi]
          (js/document.querySelector "#pixi-mount"))

;; Editor set-up
(def editor (js/ace.edit "editor"))

(-> editor
    ((fn[e] (.setTheme e "ace/theme/monokai") e))
    ((fn[e]
       (.addCommand (.-commands e)
                    (clj->js
                     {:name    "save"
                      :bindKey {:win "Ctrl-S" :mac "Cmd-S"}
                      :exec    #(raise-event :update_actor_code (get-in @state [:editor :actor-type]))}))
       e))
    .getSession
    (.setMode "ace/mode/elixir"))

;; Graphics initialization -> graphics event channel
(def graphics-event-chan
  (graphics/init
   core-chan
   (js/document.querySelector "#pixi-js")
   (- (-> js/window js/jQuery .width) 10) (- (-> js/window js/jQuery .height) 10)))

(defn component-click [component-menu x y]
  (if (m/no-menu-opened? state)
    (m/open-component-menu state component-menu x y)
    (m/close-other-menues state x y)))

(defn close-extra-components []
  (doseq [kv @state]
    (let [v (val kv)]
      (if (and
           (not= (key kv) :some-menu-opened)
           (not= (key kv) :main-menu)
           (contains? @v :open)
           (@v :open))
        (do
          (js/console.log "!!")
          (js/console.log (pr-str v))
          (js/console.log (pr-str (key kv)))
          (swap! v assoc :open false))))))

;; Core event channel handlers
(def handlers
  {:start-new-actor (fn[new-actor]
                      (channel/push "start_actor" {:type (:type new-actor)}
                                    (fn [running_actor]
                                      (put! graphics-event-chan [:new_running_actor [running_actor new-actor]]))))
   :show-code       (fn [actor-type]
                      (channel/push "get_actor_code" {:name actor-type}
                                    (fn [resp]
                                      (swap! (@state :editor) assoc :actor-type actor-type :open true)
                                      (.setValue editor (get resp "code"))
                                      ;; (.sideNav (js/jQuery ".button-collapse") "show")
                                      )))
   :open-message-input (fn [actor-pid]
                         (swap! (:send_message @state) assoc :to actor-pid)
                         (swap! (:message-input-dialog @state) assoc :open true)
                         )
   :open-new-actor-input (fn [_]
                           (swap! (:add-actor-input-dialog @state) assoc :open true))
   :send-actor-message (fn [msg]
                         (channel/push "send_msg" {:to ((deref (@state :send_message)) :to) :msg msg}
                                       (fn [resp]
                                         (js/console.log (pr-str resp))
                                         (swap! (:response @state) assoc :open true :value (pr-str resp) :header "Response")
                                         )
                                       (fn [err]
                                         (js/console.log (pr-str err))
                                         (swap! (@state :error) assoc :value (pr-str err) :open true)
                                         )))
   :new_actor_type (fn [actor_type]
                     (channel/push "new_actor" {:name actor_type}
                                   (fn [resp]
                                     (swap! (@state :response) assoc :value (pr-str resp))
                                     (put! core-chan [:show-code (get resp "actor_type")])
                                     (put! graphics-event-chan [:new_actor_type (get resp "actor_type")])
                                     )))
   :update_actor_code (fn [actor_type]
                        (channel/push "update_actor" {:name actor_type :actor_code (.getValue editor)}
                                      #(do
                                         (swap! (@state :error) assoc :open false)
                                         (swap! (@state :snackbar) assoc :open true :message "Code saved" :color (color :green500))
                                         )
                                      #(do
                                         (swap! (@state :error) assoc :value (pr-str %) :open true))))
   :message-click (fn [{x :x y :y}]
                    (js/console.log "1")
                    ;; (component-click :message-menu x y)
                    )
   :canvas-click (fn [{x :x y :y}]
                   (close-extra-components)
                   (component-click :main-menu x y)
                   )
   :running-actor-click (fn [{x :x y :y pid :pid}]
                          (swap! (:running-actor-menu @state) assoc-in [:pid] pid)
                          (component-click :running-actor-menu x y))
   :open-input-dialog (fn [id] (swap! (id @state) assoc :open true))
   :close-input-dialog (fn [id]
                         (swap! (id @state) assoc :open false ))
   })

;; Backent channel set-up
(defonce joined-chan
  (channel/join
   (fn [actor_types]
     (put! graphics-event-chan [:set_actor_types (get actor_types "actors")]))))

;; Start listening for events
(go
  (while true
    (let [[event-name event-data] (<! core-chan)]
      ((event-name handlers) event-data))))
