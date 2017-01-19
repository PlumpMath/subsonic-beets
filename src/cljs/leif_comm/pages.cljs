(ns leif-comm.pages
  (:require [reagent.core :as reagent :refer [atom]]
            [accountant.core :as accountant]
            [leif-comm.state :as state]
            [leif-comm.server-comms :as server-comms]))

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
  (server-comms/send-message! message)
  (state/append! state/chat-log @state/name-atom message)
  (reset! state/sendtextarea-atom ""))


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
  "a textarea which expands up to max-rows as it's content expands"
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

(defn chat []
  [:div {:id :chat}
   [:a {:href "/"} "< Back"]
   [atom-textarea :received state/chat-log {:disabled true}]
   [expanding-textarea {:id "chat-input"
                        :max-rows   7
                        :auto-focus false}]])
