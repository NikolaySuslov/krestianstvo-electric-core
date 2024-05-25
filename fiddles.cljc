(ns krestianstvo.fiddles
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle :as hf]
            [electric-fiddle.main]
            [krestianstvo.browser :refer [Browser]]
            [krestianstvo.counterDemo :refer [CounterDemo]]
            [krestianstvo.painterDemo :refer [PainterDemo]]
            [krestianstvo.clickerDemo :refer [ClickerDemo]]
            [krestianstvo.portalDemo :refer [PortalDemo]]
            [krestianstvo.demo :refer [Demo]]))

(e/def fiddles ; Entries for the dev index
  {`Browser Browser
   `Counter CounterDemo
   `Painter PainterDemo
   `Clicker ClickerDemo
   `Portal PortalDemo
   `Demo Demo})

(e/defn FiddleMain [ring-req]
  (e/client
    (binding [hf/pages fiddles]
      (e/server
        (electric-fiddle.main/Main. ring-req)))))