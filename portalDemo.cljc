(ns krestianstvo.portalDemo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.worlds :as w]))

(e/defn PortalDemo []
  (e/client
    (let [clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (w/DemoCreator.
        w/PortalWorld
        :portal
        clientID
        "Krestianstvo | Portal World"
        "/(krestianstvo.fiddles!Portal)"))))