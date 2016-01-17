(ns gifer.core
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojurewerkz.quartzite.scheduler :as scheduler]
            [clojurewerkz.quartzite.jobs :as jobs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.cron :as cs :refer [cron-schedule]]
            [environ.core :as env :refer [env]]))

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
        _        (io/copy in out)
        resp     @(http/post (str base-url "/sendDocument")
                             {:query-params {:chat_id chat-id}
                              :multipart [{:name "document"
                                           :content tmp-file
                                           :filename "giphy.gif"}]})]
    (:body resp)))

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

(jobs/defjob SendGifs
  [ctx]
  (println "Sending gifs...")
  (send-gifs)
  (println "Gifs sent!"))

(defn -main [& args]
  (let [s       (-> (scheduler/initialize) (scheduler/start))
        job     (jobs/build
                 (jobs/of-type SendGifs)
                 (jobs/with-identity (jobs/key "jobs.gifs.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (cs/schedule
                                   (cron-schedule "10 * * * * ?"))))]
    (scheduler/schedule s job trigger)
    (println "Starting scheduler!")))
