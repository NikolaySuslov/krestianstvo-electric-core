(ns krestianstvo.worlds
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.krestianstvo :as k]
            [krestianstvo.objects :as o]
            [krestianstvo.painter :as p]
            [krestianstvo.demoWorld :as dw]))

(e/defn WorldTemplate [seloID clientID app appID]
  (e/client
    (let [!objData (atom {})
          virtualTime  (k/Selo. seloID clientID !objData)

          !showInfo (atom false)
          showInfo (e/watch !showInfo)]

      (dom/div (dom/props {:style {:width "fit-content"
                                   :margin "auto"
                                   :border-style "dotted"
                                   :border-width "1px"
                                   :padding "5px"
                                   :position "relative"}})
        (o/Header. seloID clientID virtualTime !objData)

        (dom/div
          (dom/props {:style {:position "relative"}})

          (e/for [e (e/server (:clients (seloID k/data)))] 
            (o/Avatar. e seloID clientID !objData virtualTime))

         ;; (o/Metronome. :metronome seloID !objData virtualTime)

          (dom/div
            (dom/props {:style {:display "flex"
                                :align-items "left"
                                :justify-content "space-around"}})

            (app. appID seloID !objData virtualTime))))

      (dom/button
        (dom/props {:style {:width "200px"
                            :margin "auto"}})
        (dom/text "Info")
        (dom/on! "click" (fn [e]
                          (swap! !showInfo not))))

      (dom/div
        (dom/props {:style {:margin "auto"
                            :width "fit-content"}})
        (if showInfo
          (o/SeloInfo. seloID clientID !objData virtualTime))))))

(e/defn CounterWorld [seloID clientID]
  (e/client
    (WorldTemplate. seloID clientID o/Counter :counter)))

(e/defn ClickerWorld [seloID clientID]
  (e/client
    (WorldTemplate. seloID clientID o/Clicker :clicker)))

(e/defn PainterWorld [seloID clientID]
  (e/client
    (WorldTemplate. seloID clientID p/Painter :painter)))

(e/defn PortalWorld [seloID clientID]
  (e/client
    (let [worldsDir {:counter CounterWorld
                     :clicker ClickerWorld
                     :portal PortalWorld
                     :painter PainterWorld
                     :demo dw/DemoWorld}

          !objData (atom {})

          virtualTime  (k/Selo. seloID clientID !objData)]

      (dom/div (dom/props {:style {:border-style "dotted"
                                   :border-width "1px"
                                   :padding "5px"
                                   :position "relative"}})

        (o/Header. seloID clientID virtualTime !objData)

        (dom/div
          (dom/props {:style {:position "relative"}})

          (e/for [e (e/server (:clients (seloID k/data)))]
            (o/Avatar. e seloID clientID !objData virtualTime))

          (dom/div
            (dom/props {:style {:display "flex"
                                :align-items "left"
                                :justify-content "space-around"}})

            (o/Portal. :portal seloID !objData virtualTime worldsDir)))))))

(e/def worldsDir {:counter CounterWorld
                  :clicker ClickerWorld
                  :painter PainterWorld
                  :portal PortalWorld
                  :demo dw/DemoWorld})

(e/defn DemoCreator [world seloID clientID label link]
  (e/client
    (dom/div
      (dom/a (dom/props {:href link :target "_blank"}) (dom/text "Link to world"))
      (dom/div
        (dom/props {:style {:margin "auto"
                            :width "fit-content"}})
        (dom/h4
          (dom/text label)))

      (dom/div
        (dom/props {:style {:display "grid"
                            :align-items "center"
                            :justify-content "space-around"}})

        (if (= :demo seloID)
          (new world seloID clientID worldsDir)
          (new world seloID clientID))))))