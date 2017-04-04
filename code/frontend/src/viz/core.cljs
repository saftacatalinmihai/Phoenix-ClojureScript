(ns viz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [dommy.core :as dommy
     :refer-macros  [sel sel1]]
    [reagent.core :as r]
    [viz.channel :as channel]
    [viz.graphics :as graphics]
    [cljs.core.async :refer [put! chan <!]]))

(defonce state (atom {:code-in-editor ""}))

(defn set-actor-in-editor[actor_type]
  (swap! state assoc-in [:code-in-editor] actor_type))

(defn get-actor-in-editor[] (:code-in-editor @state))

(defn pixi []
  [:div {:id "pixi-js"}])

(r/render [pixi]
  (js/document.querySelector "#pixi-mount"))

(.sideNav (js/jQuery ".button-collapse")
  (clj->js
    {:menuWidth (/ (-> js/window js/jQuery .width) 2)
     :edge      'right'}))

(.modal (js/jQuery ".modal"))

(def editor (js/ace.edit "editor"))

(def core-chan (chan))

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
                                      (.sideNav (js/jQuery ".button-collapse") "show"))))})

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
                  (set! (.-innerHTML (js/document.querySelector "#error-modal")) %)
                  (.modal (js/jQuery "#modal1") "open"))))

(-> editor
  ((fn[e] (.setTheme e "ace/theme/monokai") e))
  ((fn[e]
     (.addCommand (.-commands e)
                  (clj->js
                    {:name    "save"
                     :bindKey {:win "Ctrl-S" :mac "Cmd-S"}
                     :exec    (fn[] (update_actor_code! (get-actor-in-editor)))}))
     e))
  .getSession
  (.setMode "ace/mode/elixir"))