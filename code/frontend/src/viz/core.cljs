(ns viz.core
  (:require
    [viz.channel :as channel]
    [viz.graphics :as graphics]
    [dommy.core :as dommy
     :refer-macros  [sel sel1]]
    [quil.core :as   q
     :include-macros true]
    [quil.middleware :as m
     :include-macros     true]))

(defn js-log [data & rest]
  (.log js/console data, rest))

(defn update_actor_code![editor]
  (channel/push
    "update_actor"
    {:name       (dommy/value (sel1 :#current-actor-code))
     :actor_code (.getValue editor)}))

(def editor (js/ace.edit "editor"))

(-> editor
  ((fn[e] (.setTheme e "ace/theme/monokai") e))
  ((fn[e]
     (.addCommand (.-commands e)
                  (clj->js
                    {:name    "save"
                     :bindKey {:win "Ctrl-S" :mac "Cmd-S"}
                     :exec    (fn[editor] (update_actor_code! editor))}))
     e))
  .getSession
  (.setMode "ace/mode/elixir"))

(def app_state (atom {}))

(defn update_curent_actor![actor_name]
  (dommy/set-value! (sel1 :#current-actor-code) actor_name)
  (dommy/add-class! (sel1 :#current-actor-code-div) "is-dirty"))

(defn on_ready[]
  (graphics/init)
  ;; TODO: Use App state to hold all state reflected in the DOM
  ;; Then make functions that take the state and render it
  (swap! app_state assoc :actor_list [])

  ;; Reset Actor List
  (dommy/set-html! (sel1 :#actor-list) "")

  (defn set_code_in_editor[code]
    (.setValue editor code))

  (defn get_actor_code![actor_name]
    (channel/push "get_actor_code"
                  {:name actor_name}
                  (fn [resp] (set_code_in_editor (get resp "code")))))

  (defn on_actor_click![e]
    (-> e
        .-srcElement
        .-innerText
        ((fn[actor_name] (update_curent_actor! actor_name) actor_name))
        get_actor_code!))

  (defn add_to_actor_list[actor_name]
    (let [elem (dommy/create-element "LI")]
      (dommy/append! (sel1 :#actor-list) (dommy/set-html! elem actor_name))
      (dommy/listen! elem :click on_actor_click!)))

  (channel/join
    (fn [resp]
      (let [actor_list (get resp "actors")]
        (println actor_list)
        (doseq [actor_name actor_list]
          (add_to_actor_list actor_name)))))

  (defn new_actor!
    [e]
    (if (== 13 (.-keyCode e))
        (do
          (println (dommy/value (sel1 :#new-actor)))
          (println "New Actor")
          (channel/push "new_actor",
                        {:name (dommy/value (sel1 :#new-actor))}
                        (fn[resp]
                          (println "Actor created ok", resp)
                          (add_to_actor_list (get resp "name")))))))

  (defn send_msg!
    [e]
    (if (== 13 (.-keyCode e))
        (do
          (println "Send msg")
          (channel/push "send_msg"
                        {:name   (dommy/value (sel1 :#send-msg-actor-name))
                         :to_pid (dommy/value (sel1 :#send-msg-actor-pid))
                         :msg    (dommy/value (sel1 :#send-msg-msg))}))))

  (defn update_actor_code_on_enter!
    [e]
    (if (== 13 (.-keyCode e))
        (do
          (println "Update actor code")
          (update_actor_code! editor))))

  (dommy/listen! (sel1 :#new-actor) :keyup new_actor!)
  (dommy/listen! (sel1 :#send-msg-msg) :keyup send_msg!))

(js/jQuery (fn[] (on_ready)))

