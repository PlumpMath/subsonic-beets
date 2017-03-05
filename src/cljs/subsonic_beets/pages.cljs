(ns subsonic-beets.pages
  (:require [reagent.core :as reagent :refer [atom]]
            [accountant.core :as accountant]
            [subsonic-beets.state :as state]
            [subsonic-beets.server-comms :as server-comms]))

(enable-console-print!)
(defn atom-field [atom placeholder type]
  [:input {:type type
           :value @atom
           :placeholder placeholder
           :on-change #(reset! atom (-> % .-target .-value))}])

(defn atom-textarea [id atom opts]
  [:textarea (merge {:id id
                     :value @atom
                     :on-change #(reset! atom (-> % .-target .-value))}
                    opts)])


(defn home []
  [:div [:h2 "Welcome to LEIF-comm!"]
   [:div [:a {:href "/about"} "About"]]
   [:div [:a {:href "/register"} "Register"]]
   [atom-field state/name-atom "Nickname" "text"]
   [:input {:type "button" :value "Start" :on-click
            #(do (server-comms/anonymous-login! @state/name-atom)
                 (accountant/navigate! "/chat"))}]])


(defn send-chat!
  [message]
  (server-comms/send-message! message))


(defn update-rows
  [row-count-atom max-rows dom-node value]
  (let [field-height   (.-clientHeight dom-node)
        content-height (.-scrollHeight dom-node)]
    (cond
      (and (not-empty value)
           (> content-height field-height)
           (< @row-count-atom max-rows))
      (swap! row-count-atom inc)

      (empty? value)
      (reset! row-count-atom 1))))

(defn expanding-textarea
  "A textarea which expands with the inputted text."
  [{:keys [max-rows] :as opts}]
  (let [dom-node      (atom nil)
        row-count     (atom 1)
        written-text  (atom "")
        enter-keycode 13]
    (reagent/create-class
     {:display-name "expanding-textarea"

      :component-did-mount
      (fn [ref]
        (reset! dom-node (reagent/dom-node ref))
        (update-rows row-count max-rows @dom-node @written-text))

      :component-did-update
      (fn []
        (update-rows row-count max-rows @dom-node @written-text))

      :reagent-render
      (fn [opts]
        (let [opts (dissoc opts :max-rows)]
          [:textarea
           (merge opts
                  {:rows  @row-count
                   :value @written-text
                   :on-change (fn [e]
                                (reset! written-text (-> e .-target .-value)))
                   :on-key-down (fn [e]
                                  (let [key-code  (.-keyCode e)]
                                    (when (and (= enter-keycode key-code)
                                               (not (.-shiftKey e))
                                               (not (.-altKey e))
                                               (not (.-ctrlKey e))
                                               (not (.-metaKey e)))
                                      (do
                                        (.preventDefault e)
                                        (send-chat! @written-text)
                                        (reset! written-text "")))))})]))})))


(defn chat-log-entry
  [{:keys [message-id author text acked backlog] :as data}]
  [:li {:on-click #(server-comms/ack-entry message-id)
       :class (str "chat-log-entry " acked backlog)}
   (str author ": " text)])

(defn scroll-to-bottom
  "Scroll the given dom-node to the bottom."
  [dom-node]
  (let [last-child-index (- (.-childElementCount dom-node) 1)
        last-child    (aget (.-childNodes dom-node) last-child-index)]
    (when last-child
      (.scrollIntoView last-child (clj->js {:behavior "smooth"})))))

(defn chat-log
  []
  (let [dom-node-atom (atom nil)]
    (reagent/create-class
     {:display-name "chat-log"
      :component-did-mount  #(reset! dom-node-atom (reagent/dom-node %))
      :component-did-update #(scroll-to-bottom @dom-node-atom)
      :reagent-render
      (fn [opts]
        [:ul {:id "received"}
         (for [c (vals @state/chat-log)]
           ^{:key (:message-id c)} [chat-log-entry c])])})))

(defn chat []
  [:div {:id :chat}
   [:a {:href "/"} "< Back"]
   [chat-log]
   [expanding-textarea {:id "chat-input"
                        :max-rows   7
                        :auto-focus false}]])
