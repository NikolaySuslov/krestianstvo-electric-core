(ns krestianstvo.counterDemo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.worlds :as w]))

(e/defn CounterDemo []
  (e/client
    (let [clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (w/DemoCreator.
        w/CounterWorld
        :counter
        clientID
        "Krestianstvo | Counter World"
        "/(krestianstvo.fiddles!Counter)"))))
