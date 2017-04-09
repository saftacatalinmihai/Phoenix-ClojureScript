(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [dommy.core :as dommy :refer-macros  [sel sel1]]
   [reagent.core :as r]
   ;; cljsjs.ace
   ;; cljsjs.jquery
   [viz.channel :as channel]
   [viz.graphics :as graphics]
   [cljs.core.async :refer [put! chan <!]]))

;; REAGENT STATE
(defonce state (r/atom {
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
  [:div {:id "error-modal" :class (str "modal bottom-sheet")}
   [:div {:class "modal-content"}
    [:h5 "Error"]
    [:p {:id "#code-error-modal"} (:error @state)]]])

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
   :open-message-modal (fn [actor]
                         (swap! state assoc-in [:send_message :to] (:pid actor))
                         (.modal (js/jQuery "#modal-send-message") "open"))
   :add-new-actor (fn [_]
                    (.modal (js/jQuery "#modal-new-actor") "open"))
   :send-actor-message (fn [msg]
                         (js/console.log (pr-str msg))
                         (channel/push "send_msg" msg
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
                                         (.modal (js/jQuery "#error-modal") "open"))) )
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


