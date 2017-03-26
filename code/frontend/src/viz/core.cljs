(ns viz.core)

(.log js/console "Hey Seymore sup?!")

(def socket (new js/Phoenix.Socket
;                 (str "ws://" (.getElementById js/document "host") ":" (.getElementById js/document "port") "/socket" )
                 "ws://localhost:4001/socket"
                 (clj->js {:params {:token :window.userToken}})))

(.connect socket)

(def channel (.channel socket "room:lobby" (clj->js {})))

(def joinedChannel (.join channel))
(.receive joinedChannel "ok" (fn[resp] (.log js/console "Joined successfully", resp)))
(.receive joinedChannel "error" (fn[resp] (.log js/console "Unable to join", resp)))
