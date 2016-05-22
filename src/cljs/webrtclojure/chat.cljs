(ns webrtclojure.chat)

(defonce recvtextarea-atom (atom ""))

(defn append! [user message]
    (swap! recvtextarea-atom str (str user ":\n" message "\n")))