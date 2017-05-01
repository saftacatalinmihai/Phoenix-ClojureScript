(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljsjs.material-ui]
    [cljs-react-material-ui.core :refer [get-mui-theme color]]
    [cljs-react-material-ui.reagent :as ui]
    [cljs-react-material-ui.icons :as ic]
    [cljsjs.codemirror]
    [cljsjs.jquery]
    [reagent.core :as r]
    [viz.channel :as channel]
    [viz.graphics2 :as graphics]
    [viz.menues :as m]
    [cljs.core.async :refer [put! chan <!]]))

;; Core event channel
(def core-chan (chan))
(defn raise-event [event-name event-data]
      (put! core-chan [event-name event-data]))

;; REAGENT STATE
(defonce state
         (r/atom
           {
            :message-input-dialog     (r/atom
                                        {:id     :message-input-dialog
                                         :title  "Message"
                                         :open   false
                                         :action (fn [st value]
                                                     (put! core-chan [:send-actor-message value])
                                                     (swap! st assoc :open false))})

            :add-actor-input-dialog   (r/atom
                                        {:id     :add-actor-input-dialog
                                         :title  "Add actor"
                                         :open   false
                                         :action (fn [st value]
                                                     (put! core-chan [:new_actor_type value])
                                                     (swap! st assoc :open false))})

            :new-message-input-dialog (r/atom
                                        {
                                         :id     :new-message-input-dialog
                                         :title  "New Message"
                                         :open   false
                                         :x      400
                                         :y      400
                                         :action (fn [st value]
                                                     (put! core-chan [:new-message {:msg value :x (@st :x) :y (@st :y)}])
                                                     (swap! st assoc :open false))})



            :some-menu-opened         (r/atom {:open false :x 0 :y 0})
            :message-menu             (r/atom {:x 0 :y 0 :msg "" :open false})
            :main-menu                (r/atom {:x 0 :y 0 :open false})
            :running-actor-menu       (r/atom {:x 5 :y 5 :open false})
            :editor                   (r/atom {:actor-type "" :open false :code ""})
            :response                 (r/atom
                                        {:header "Response" :value "" :open false :color (color :green600)})
            :error                    (r/atom {:header "Error" :value "" :open false :color (color :red500)})
            :send_message             (r/atom {:to "" :msg ""})
            :new-actor                (r/atom {:type ""})
            :snackbar                 (r/atom {:message "" :open false})}))


;; Reagent components
(defn pixi []
      [:div {:id "pixi-js"}])

(defn slide-out-editor [event-channel state]
      [ui/mui-theme-provider {:mui-theme (get-mui-theme (aget js/MaterialUIStyles "DarkRawTheme"))}
       [ui/drawer {
                   :open              (@state :open)
                   :open-secondary    true
                   :width             (/ (-> js/window js/jQuery .width) 2)
                   :docked            false
                   :on-request-change #(if (= % false) (swap! state assoc :open false))}

        [:div {:id "editor"}]]])

(defn input-dialog [event-channel state]
      (let [value (atom "")
            on-enter #(if (= 13 (.-charCode %)) (%2))]
           [ui/mui-theme-provider {get-mui-theme {:palette {:text-color (color :green600)}}}
            [:div
             [ui/dialog {:title            (:title @state)
                         :actions          [(r/as-element
                                              [ui/flat-button
                                               {:label        "Submit"
                                                :primary      true
                                                :on-touch-tap #(do
                                                                 (js/console.log @state)
                                                                 ((@state :action) state @value))}])]

                         :open             (:open @state)
                         :on-request-close #(swap! state assoc :open false)}

              [ui/text-field {
                              :id            "input-dialog"
                              :hint-text     ""
                              :default-value @value
                              :on-change     #(reset! value %2)
                              :on-key-press  (fn [e] (on-enter e #((@state :action) state @value)))}]]]]))

(defn bottom-resp [state]
      [ui/mui-theme-provider {get-mui-theme {:palette {:text-color (color :green600)}}}

       [ui/paper {:style (clj->js
                           {:display  (if (= true (get @state :open)) "inline-block" "none")
                            :position "absolute"
                            :bottom   "0px"
                            :width    "100%"})}

        [ui/text-field {:id              "response"
                        :full-width      true
                        :value           (@state :value)
                        :underline-style {:border-color (@state :color)}}]]])

(defn snack-bar [state]
      [ui/mui-theme-provider
       [ui/snackbar {:open               (@state :open)
                     :message            (@state :message)
                     :auto-hide-duration 4000
                     :content-style      {:color (get @state :color (color :white500))}
                     :on-request-close   #(swap! state assoc :open false)}]])

(defn reagent-mount []
      [:div
       [pixi]
       [m/main-menu core-chan (:main-menu @state)]
       [m/running-actor-menu core-chan (:running-actor-menu @state)]
       [input-dialog core-chan (:message-input-dialog @state)]
       [input-dialog core-chan (:add-actor-input-dialog @state)]
       [input-dialog core-chan (:new-message-input-dialog @state)]
       [slide-out-editor core-chan (@state :editor)]
       [bottom-resp (@state :response)]
       [bottom-resp (@state :error)]
       [snack-bar (@state :snackbar)]])

(r/render [reagent-mount]
          (js/document.querySelector "#mount"))

;; Editor set-up
(def editor
  (js/CodeMirror.
    (.querySelector js/document "#editor")
    (clj->js {
              :mode           "elixir"
              :theme          "monokai"
              :viewportMargin "500"
              :extraKeys      {"Ctrl-S" #(put! core-chan [:update_actor_code ((deref (@state :editor)) :actor-type)])}
              :lineNumbers    true
              :value          ((deref (@state :editor)) :code)
              })
    ))

(defn graphics-init [{
                      init-fn         :init-fn
                      core-ch         :core-ch
                      mount-el        :mount-el
                      width           :width
                      height          :height
                      g-new-box-fn    :g-new-box-fn
                      g-new-sphere-fn :g-new-sphere-fn
                      }]
      (let [
            graphics-ch (chan)
            graphics (init-fn core-ch mount-el width height)
            handlers {:new-box         g-new-box-fn
                      :new-sphere      g-new-sphere-fn
                      :set_actor_types #(println %2)}
            ]
           (go
             (while true
                    (let [[ev-name ev-data] (<! graphics-ch)]
                         ((get handlers ev-name) graphics ev-data))))
           graphics-ch
           )
      )

;; Graphics initialization -> graphics event channel
(def graphics-event-chan
  (graphics-init {:init-fn         graphics/init
                  :core-ch         core-chan
                  :mount-el        (js/document.querySelector "#pixi-js")
                  :width           (- (-> js/window js/jQuery .width) 10)
                  :height          (- (-> js/window js/jQuery .height) 10)
                  :g-new-box-fn    graphics/new-box
                  :g-new-sphere-fn graphics/new-sphere})
  ;(graphics/init
  ;  core-chan
  ;  (js/document.querySelector "#pixi-js")
  ;  (- (-> js/window js/jQuery .width) 10) (- (-> js/window js/jQuery .height) 10))
  )

;(graphics/dispatch {:type :move-x :increment -10})

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
  {:start-new-actor      (fn [new-actor]
                             (channel/push "start_actor" {:type (:type new-actor)}
                                           (fn [running_actor]
                                               (put! graphics-event-chan [:new_running_actor [running_actor new-actor]]))))
   :show-code            (fn [actor-type]
                             (channel/push "get_actor_code" {:name actor-type}
                                           (fn [resp]
                                               (swap! (@state :editor) assoc :actor-type actor-type :open true :code (get resp "code"))
                                               (.setValue editor (get resp "code")))))

   :open-message-input   (fn [actor-pid]
                             (swap! (:send_message @state) assoc :to actor-pid)
                             (swap! (:message-input-dialog @state) assoc :open true))

   :open-new-actor-input (fn [_]
                             (swap! (:add-actor-input-dialog @state) assoc :open true))
   :send-actor-message2  (fn [{to :to msg :msg}]
                             (js/console.log (pr-str to msg))
                             (channel/push "send_msg" {:to to :msg msg}
                                           (fn [resp]
                                               (js/console.log (pr-str resp))
                                               (swap! (:response @state) assoc :open true :value (pr-str resp) :header "Response"))

                                           (fn [err]
                                               (js/console.log (pr-str err))
                                               (swap! (@state :error) assoc :value (pr-str err) :open true))))


   :send-actor-message   (fn [msg]
                             (channel/push "send_msg" {:to ((deref (@state :send_message)) :to) :msg msg}
                                           (fn [resp]
                                               (js/console.log (pr-str resp))
                                               (swap! (:response @state) assoc :open true :value (pr-str resp) :header "Response"))

                                           (fn [err]
                                               (js/console.log (pr-str err))
                                               (swap! (@state :error) assoc :value (pr-str err) :open true))))

   :new_actor_type       (fn [actor_type]
                             (channel/push "new_actor" {:name actor_type}
                                           (fn [resp]
                                               (swap! (@state :response) assoc :value (pr-str resp))
                                               (put! core-chan [:show-code (get resp "actor_type")])
                                               (put! graphics-event-chan [:new_actor_type (get resp "actor_type")]))))

   :update_actor_code    (fn [actor_type]
                             (channel/push "update_actor" {:name actor_type :actor_code (.getValue editor)}
                                           #(do
                                              (swap! (@state :error) assoc :open false)
                                              (swap! (@state :snackbar) assoc :open true :message "Code saved" :color (color :green500)))

                                           #(do
                                              (swap! (@state :error) assoc :value (pr-str %) :open true))))
   :message-click        (fn [{x :x y :y msg :msg}]
                             (swap! (@state :message-menu) assoc :msg msg)
                             (js/console.log msg))
   ;; (component-click :message-menu x y)

   :canvas-click         (fn [{x :x y :y}]
                             (close-extra-components)
                             (component-click :main-menu x y))

   :running-actor-click  (fn [{x :x y :y pid :pid}]
                             (swap! (:running-actor-menu @state) assoc-in [:pid] pid)
                             (component-click :running-actor-menu x y))
   :open-input-dialog    (fn [id] (swap! (id @state) assoc :open true))
   :close-input-dialog   (fn [id]
                             (swap! (id @state) assoc :open false))
   :make-new-message     (fn [{x :x y :y}]
                             (js/console.log "xy" (pr-str x y))
                             (swap! (@state :new-message-input-dialog) assoc :open true :x x :y y))

   :new-message          (fn [{msg :msg x :x y :y}]
                             (put! graphics-event-chan [:new_message {:x x :y y :msg msg}]))})



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
