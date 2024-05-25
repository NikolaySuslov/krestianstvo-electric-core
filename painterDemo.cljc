(ns krestianstvo.painterDemo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.worlds :as w]))

(e/defn PainterDemo []
  (e/client
    (let [clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (w/DemoCreator.
        w/PainterWorld
        :painter
        clientID
        "Krestianstvo | Painter World"
        "/(krestianstvo.fiddles!Painter)"))))