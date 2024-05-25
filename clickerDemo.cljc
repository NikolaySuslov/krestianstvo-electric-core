(ns krestianstvo.clickerDemo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.worlds :as w]))

(e/defn ClickerDemo []
  (e/client
    (let [clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (w/DemoCreator.
        w/ClickerWorld :clicker clientID
        "Krestianstvo | Clicker World"
        "/(krestianstvo.fiddles!Clicker)"))))