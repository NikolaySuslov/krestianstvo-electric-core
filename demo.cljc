(ns krestianstvo.demo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.worlds :as w]
            [krestianstvo.demoWorld :as dw]))

(e/defn Demo []
  (e/client
    (let [clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (w/DemoCreator.
        dw/DemoWorld
        :demo
        clientID
        "Krestianstvo | World Demo"
        "/(krestianstvo.demo!Demo)"))))