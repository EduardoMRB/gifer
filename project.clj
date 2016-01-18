(defproject gifer "0.1.0"
  :description "Telegram bot that sends random GIFs"
  :url "http://github.com/eduardomrb/gifer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.5.0"]
                 [environ "1.0.1"]
                 [im.chit/cronj "1.4.4"]]
  :aot [gifer.core]
  :main gifer.core)
