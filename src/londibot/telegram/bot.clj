(ns londibot.telegram.bot
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [londibot.core.api :as bot]
            [londibot.core.messages :as msg])
  (:gen-class))

(def token (env :telegram-token))

(def telegram-service-name "telegram")

(defn send-markdown-message [id msg]
  (t/send-text token id {:parse_mode "Markdown"} msg))

(defn new-telegram-job [id cron-expr]
  (bot/new-job id cron-expr telegram-service-name))

(h/defhandler handler
  (h/command-fn
   "start"
   (fn
     [{{id :id, name :first_name} :chat}]
     (send-markdown-message id (str "Hi " name "! Welcome to londibot! I am your humble TFL services servant :)"))))

  (h/command-fn
   "help"
   (fn
     [{{id :id} :chat}]
     (send-markdown-message id (msg/default-help-message))))

  (h/command-fn
   "status"
   (fn
     [{{id :id} :chat}]
     (bot/send-status-notification (fn [text] (send-markdown-message id text)))))

  (h/command-fn
   "schedule"
   (fn
     [{{id :id} :chat, cron-expr :text}]
     (let [job (new-telegram-job id (subs cron-expr 9))] ; We want to trim the "/schedule" command from the string.
       (bot/create-scheduled-status-notification job (fn [reply] (send-markdown-message id reply))))))

  (h/message-fn
   (fn
     [{{id :id} :chat}]
     (send-markdown-message id "To see what I can do for you, use the `/help` command."))))


(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provide token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the londibot")
  (bot/schedule-all-notifications telegram-service-name send-markdown-message)
  (<!! (p/start token handler)))
