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
   [viz.menues :refer [running-actor-menu main-menu]]
   [cljs.core.async :refer [put! chan <!]]))

;; REAGENT STATE
(defonce state (r/atom {
                        :message-input-dialog (r/atom {:id :message-input-dialog :title "Message" :open false} )
                        :some-menu-opened {:open false :x 0 :y 0}
                        :main-menu (r/atom {:x 0 :y 0 :open false})
                        :running-actor-menu (r/atom {:x 5 :y 5 :open false})
                        :editor {:actor-type ""}
                        :response {:header "" :value ""}
                        :error ""
                        :send_message {:to "" :msg ""}
                        :new-actor {:type ""}
                        }))

;; Core event channel
(def core-chan (chan))
(defn raise-event [event-name event-data]
  (put! core-chan [event-name event-data]))

;; Reagent components
(defn pixi []
  [:div {:id "pixi-js"}])

(defn slide-out-editor []
  [:div
   [:ul {:id "slide-out" :class "side-nav"}
    [:div {:id "editor"}]]
   [:a {:href "#" :data-activates "slide-out" :class "button-collapse"}]])

(defn error-modal []
  [:div
   [:raised-button {:label "Dialog" :on-touch-tap (fn [event value]
                                                    (js/console.log event)
                                                    (js/console.log value)
                                                    (swap! state assoc-in [:error-open] true)
                                                    )}]]
  [:dialog {
            :id "error-modal"
            :title "Error"
            :open (get @state :error-open)
            :class (str "modal bottom-sheet")
            }
   [:h5 "Error"]
   [:p {:id "#code-error-modal"} (:error @state)]])

(defn input-dialog [event-channel state]
  (js/console.log state)
  (let [value (atom "")]
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme
                  {:palette {:text-color (color :green600)}})}
     [:div
      [ui/dialog {:title (:title @state)
                  :actions [(r/as-element
                             [ui/flat-button
                              {:label "Submit"
                               :primary true
                               :on-touch-tap #(do
                                                (put! event-channel [:send-actor-message @value])
                                                (put! event-channel [:close-input-dialog (:id @state)])
                                                )}])]
                  :open (:open @state)
                  :on-request-close #(put! event-channel [:close-input-dialog (:id @state)])
                  }
       [ui/text-field {
                       :hint-text ""
                       :default-value @value
                       :on-change #(reset! value %2)
                       }]]]]))

(defn bottom-input-modal [header label id state-key-list on-enter]
  [:div {:id (str "modal-" id) :class "modal bottom-sheet"}
   [:div {:class "modal-content"}
    [:h4 header]
    [:div {:class "input-field"}
     [:input {
              :id id
              :type "text"
              :value (get-in @state state-key-list "")
              :on-change #(swap! state assoc-in state-key-list (-> % .-target .-value))
              :on-key-press (fn [e]
                              (if (= 13 (.-charCode e))
                                (on-enter (get-in @state state-key-list ""))))
              }]
     [:label {:for id} label]]
    ]])

(defn bottom-modal-resp [{header :header value :value}]
  [:div {:id (str "modal-resp") :class "modal bottom-sheet"}
   [:div {:class "modal-content"}
    [:h4 header]
    [:textarea
     {:disabled true :value value}]]])

(defn add-new-actor-modal []
  (bottom-input-modal
   "Add new actor type"
   "Type"
   "new-actor"
   [:new-actor :type]
   (fn [input] (raise-event :new_actor_type {:name input}))))

(defn send-message-modal []
  (bottom-input-modal
   "Send Message"
   "Message"
   "send-message"
   [:send_message :msg]
   (fn [input] (raise-event :send-actor-message (get @state :send_message )))))

(defn reagent-mount []
  [:div
   [main-menu core-chan (:main-menu @state)]
   [running-actor-menu core-chan (:running-actor-menu @state)]
   [input-dialog core-chan (:message-input-dialog @state)]
   [slide-out-editor]
   [error-modal]
   [send-message-modal]
   [bottom-modal-resp (get-in @state [:response] )]
   [add-new-actor-modal]
   ])

(r/render [reagent-mount]
          (js/document.querySelector "#reagent-mount"))

(r/render [pixi]
          (js/document.querySelector "#pixi-mount"))

;; JavaScript library functions ( set-up )
(.sideNav (js/jQuery ".button-collapse")
          (clj->js
           {:menuWidth (/ (-> js/window js/jQuery .width) 2)
            :edge      'right'}))

(.modal (js/jQuery ".modal"))

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

(defn no-menu-opened? [] (not (get-in @state [:some-menu-opened :open])))
(defn some-menu-opened? [] (get-in @state [:some-menu-opened :open]))
(defn open-component-menu [component-menu x y]
  (js/console.log (pr-str  "open component" component-menu))
  (js/console.log (pr-str (get @state component-menu)))
  (swap! (get @state component-menu) #(-> % (assoc :x x :y y :open true)))
  (swap! state assoc-in [:some-menu-opened] {:open true :x x :y y})
)
(defn close-other-menues [x y]
  (js/console.log "close others")
  (swap! (:main-menu @state) assoc-in [:open] false)
  (swap! (:running-actor-menu @state) assoc-in [:open] false)
  (swap! state assoc-in [:some-menu-opened] {:open false :x x :y y})
)
(defn menues-eq-xy [m1 m2]
  (and (= (:x m1) (:x m2))) (= (:y m1) (:y m2)))

(defn component-click [component-menu x y]
  (if (no-menu-opened?)
    (open-component-menu component-menu x y)
    (close-other-menues x y)))

;; Core event channel handlers
(def handlers
  {:start-new-actor (fn[new-actor]
                      (channel/push "start_actor" {:type (:type new-actor)}
                                    (fn [running_actor]
                                      (put! graphics-event-chan [:new_running_actor [running_actor new-actor]]))))
   :show-code       (fn [actor-type]
                      (channel/push "get_actor_code" {:name actor-type}
                                    (fn [resp]
                                      (swap! state assoc-in [:editor :actor-type] actor-type)
                                      (.setValue editor (get resp "code"))
                                      (.sideNav (js/jQuery ".button-collapse") "show")
                                      )))
   :open-message-input (fn [actor-pid]
                         (swap! state assoc-in [:send_message :to] actor-pid)
                         (swap! (:message-input-dialog @state) assoc :open true)
                         )
   :add-new-actor (fn [_]
                    (.modal (js/jQuery "#modal-new-actor") "open"))
   :send-actor-message (fn [msg]
                         (js/console.log (pr-str msg))
                         (channel/push "send_msg" {:to (get-in @state [:send_message :to]) :msg msg}
                                       (fn [resp]
                                         (swap! state assoc-in [:response] {:value (pr-str resp) :header "Response"})
                                         (.modal (js/jQuery "#modal-resp") "open"))
                                       (fn [err]
                                         (swap! state assoc-in [:error] err)
                                         (.modal (js/jQuery "#error-modal") "open"))))
   :new_actor_type (fn [msg]
                     (channel/push "new_actor" msg
                                   (fn [resp]
                                     (swap! state assoc-in [:response :value] (pr-str resp))
                                     (.modal (js/jQuery "#modal-new-actor") "close")
                                     (put! core-chan [:show-code (:name msg)])
                                     (put! graphics-event-chan [:new_actor_type (:name msg)])
                                     )))
   :update_actor_code (fn [actor_type]
                        (channel/push "update_actor" {:name actor_type :actor_code (.getValue editor)}
                                      #(do
                                         (.modal (js/jQuery "#error-modal") "close")
                                         (js/Materialize.toast "Code saved" 4000 "green"))
                                      #(do
                                         (swap! state assoc-in [:error] %)
                                         (.modal (js/jQuery "#error-modal") "open"))))
   :message-click (fn [{x :x y :y}]
                    (js/console.log "1")
                    ;; (component-click :message-menu x y)
                    )
   :canvas-click (fn [{x :x y :y}]
                   (component-click :main-menu x y))
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
