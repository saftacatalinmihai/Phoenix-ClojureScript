(ns viz.core
  (:use-macros
    [dommy.macros :only [node sel sel1]])
  (:require
    [dommy.core :as dommy]))

(.log js/console "Hey Seymore sup?!")

(def socket (new js/Phoenix.Socket
                 "ws://localhost:4001/socket"
                 (clj->js {:params {:token :window.userToken}})))

(.connect socket)

(def channel (.channel socket "room:lobby" (clj->js {})))

(def joinedChannel (.join channel))
(.receive joinedChannel "ok" (fn[resp] (.log js/console "Joined successfully", resp)))
(.receive joinedChannel "error" (fn[resp] (.log js/console "Unable to join", resp)))

(defn channel_push[msg_type msg_body]
  (.push channel msg_type (clj->js msg_body)))

(defn new_actor!
  [e]
  (if ( == 13 (.-keyCode e))
      (do
        (.log js/console ( dommy/value (sel1 :#new-actor)))
        (.log js/console "New Actor")
        (.receive
          (channel_push "new_actor",
                        {:name ( dommy/value (sel1 :#new-actor))})
          "ok" (fn[resp] (.log js/console "Actor created ok", resp)))
      )))

(dommy/listen! (sel1 :#new-actor) :keyup new_actor!)


