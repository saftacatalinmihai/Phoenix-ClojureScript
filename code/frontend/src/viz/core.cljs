(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [dommy.core :as dommy
    :refer-macros  [sel sel1]]
   [reagent.core :as r]
   [viz.channel :as channel]
   [viz.graphics :as graphics]
   [cljs.core.async :refer [put! chan <!]]))

(defonce state (r/atom {
                        :code-in-editor ""
                        :response {:header "" :value ""}
                        :error ""
                        :send_message {:to "" :msg "" :type "" :resp ""}
                        :new-actor {:type "" :resp ""}
                        }))

(def core-chan (chan))

(defn set-actor-in-editor[actor_type]
  (swap! state assoc-in [:code-in-editor] actor_type))

(defn get-actor-in-editor[] (:code-in-editor @state))

(defn pixi []
  [:div {:id "pixi-js"}])

(defn slide-out-editor []
  [:div
   [:ul {:id "slide-out" :class "side-nav"}
    [:div {:id "editor"}]]
   [:a {:href "#" :data-activates "slide-out" :class "button-collapse"}]])

(defn code-error-modal []
  [:div {:id "modal1" :class "modal bottom-sheet"}
   [:div {:class "modal-content"}
    [:h5 "Error"]
    [:p {:id "#code-error-modal"} (:error @state)]]])

(defn send-message-form []
  [:div {:class "row"}
   [:div {:class "col s6 input-field"}
    [:input {
             :placeholder "ping"
             :id "message"
             :type "text"
             :class "validate"
             :value (get-in @state [:send_message :msg] "")
             :on-change #(swap! state assoc-in [:send_message :msg] (-> % .-target .-value))
             :on-key-press (fn [e]
                             (if (= 13 (.-charCode e))
                               (let [msg {
                                          :to_pid (:to (:send_message @state))
                                          :msg (:msg (:send_message @state))
                                          :name (:type (:send_message @state))
                                          } ]
                                 (channel/push "send_msg" msg
                                               #(swap! state assoc-in [:response :value] (pr-str %))))))
             }]
    [:label {:for "message"} "Message:"]]
   [:div {:class "col s6"}
    [:textarea
     {:disabled true :value (get-in @state [:response :value] "")}]]]
  )

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
   (fn [input]
     (let [msg {:name input}]
       (channel/push "new_actor" msg
                     (fn [resp]
                       ;; TODO: Move all effects in :handler
                       (swap! state assoc-in [:response :value] (pr-str resp))
                       (.modal (js/jQuery "#modal-new-actor") "close")
                       (put! core-chan [:show-code input])))))))

(defn send-message-modal []
  (bottom-input-modal
   "Send Message"
   "Message"
   "send-message"
   [:send_message :msg]
   (fn [input]
     (let [msg {
                :to_pid (:to (:send_message @state))
                :msg (:msg (:send_message @state))
                :name (:type (:send_message @state))
                } ]
       (channel/push "send_msg" msg
                     (fn [resp]
                       (swap! state assoc-in [:response :value] (pr-str resp))
                       (swap! state assoc-in [:response :header] "Response")
                       (.modal (js/jQuery "#modal-send-message") "close")
                       (.modal (js/jQuery "#modal-resp") "open"))
                    (fn [err] 
                      (swap! state assoc-in [:error] err)
                      (.modal (js/jQuery "#modal1") "open"))

)))))

(defn reagent-mount []
  [:div
   [slide-out-editor]
   [code-error-modal]
   [send-message-modal]
   [bottom-modal-resp (get-in @state [:response] )]
   [add-new-actor-modal]
   ])

(r/render [reagent-mount]
          (js/document.querySelector "#reagent-mount"))

(r/render [pixi]
          (js/document.querySelector "#pixi-mount"))


(.sideNav (js/jQuery ".button-collapse")
          (clj->js
           {:menuWidth (/ (-> js/window js/jQuery .width) 2)
            :edge      'right'}))

(.modal (js/jQuery ".modal"))

(def editor (js/ace.edit "editor"))


(def graphics-event-chan
  (graphics/init
   core-chan
   (js/document.querySelector "#pixi-js")
   (- (-> js/window js/jQuery .width) 10) (- (-> js/window js/jQuery .height) 10)))

(def handlers
  {:start-new-actor (fn[new-actor]
                      (channel/push "start_actor" {:type (:type new-actor)}
                                    (fn [running_actor]
                                      (put! graphics-event-chan [:new_running_actor [running_actor new-actor]]))))
   :show-code       (fn [actor-type]
                      (channel/push "get_actor_code" {:name actor-type}
                                    (fn [resp]
                                      (set-actor-in-editor actor-type)
                                      (.setValue editor (get resp "code"))
                                      (.sideNav (js/jQuery ".button-collapse") "show"))))
   :open-message-modal (fn [actor]
                         (swap! state assoc-in [:send_message :to] (:pid actor))
                         (swap! state assoc-in [:send_message :type] (:type actor))
                         (.modal (js/jQuery "#modal-send-message") "open"))
   :add-new-actor (fn [_]
                    (js/console.log "++")
                    (.modal (js/jQuery "#modal-new-actor") "open"))
   })

(go
  (while true
    (let [[event-name event-data] (<! core-chan)]
      ((event-name handlers) event-data))))

(defonce joined-chan
  (channel/join
   (fn [actor_types]
     (put! graphics-event-chan [:set_actor_types (get actor_types "actors")]))))


(defn update_actor_code![actor_type]
  (channel/push "update_actor" {:name actor_type :actor_code (.getValue editor)}
                #(do
                   (.modal (js/jQuery "#modal1") "close")
                   (js/Materialize.toast "Code saved" 4000 "green"))
                #(do
                   (swap! state assoc-in [:error] %)
                   (.modal (js/jQuery "#modal1") "open"))))

(-> editor
    ((fn[e] (.setTheme e "ace/theme/monokai") e))
    ((fn[e]
       (.addCommand (.-commands e)
                    (clj->js
                     {:name    "save"
                      :bindKey {:win "Ctrl-S" :mac "Cmd-S"}
                      :exec    #(update_actor_code! (get-actor-in-editor))}))
       e))
    .getSession
    (.setMode "ace/mode/elixir"))
