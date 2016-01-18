(ns gifer.core
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [environ.core :as env :refer [env]]
            [cronj.core :as cronj])
  (:gen-class))

(def base-url
  (str "https://api.telegram.org/bot" (env :bot-token)))

(def chat-ids [90055494 110774235])
(def subjects ["bb8"
               "star wars"
               "love"
               "marriage"
               "adventure+time"
               "jake+the+dog"
               "lord+of+the+rings"
               "puppey"
               "rabbit"
               "kitten"
               "cats"
               "the+force+awakens"])
(def giphy-key "dc6zaTOxFJmzC") ; Open beta key.
(def giphy-api "http://api.giphy.com/v1/gifs")
(def giphy-url "http://api.giphy.com/v1/gifs/search")

(defn send-gif [chat-id gif-url]
  (let [tmp-file (io/file "/tmp/giphy.gif")
        in       (io/input-stream gif-url)
        out      (io/output-stream tmp-file)
        _        (io/copy in out)]
    (http/post (str base-url "/sendDocument")
               {:query-params {:chat_id chat-id}
                :multipart [{:name "document"
                             :content tmp-file
                             :filename "giphy.gif"}]})))

(defn get-gifs [subject]
  (let [resp @(http/get giphy-url
                        {:query-params {:q subject
                                        :api_key giphy-key}})]
    (:data (json/parse-string (:body resp) true))))

(defn select-gif [gifs]
  (get-in (rand-nth gifs) [:images :downsized :url]))

(defn get-random-gif [subject]
  (let [resp @(http/get (str giphy-api "/random")
                        {:query-params {:api_key giphy-key
                                        :tag subject}})]
    (get-in resp [:data :url])))

(defn send-gifs []
  (let [gif-url (-> (rand-nth subjects)
                    (get-gifs)
                    (select-gif))]
    (doseq [chat-id chat-ids]
      (println "Sending GIF: " gif-url " to chat: " chat-id)
      (send-gif chat-id gif-url))))

(defn gif-handler [t opts]
  (println t)
  (send-gifs))

(def gif-task
  {:id "gif_task"
   :handler gif-handler
   :schedule "* /10 * * * * *"
   :opts {}})

(defn -main [& args]
  (let [cj (cronj/cronj :entries [gif-task])]
    (println "Starting scheduler!")
    (cronj/start! cj)
    (println "Scheduler started!")))
