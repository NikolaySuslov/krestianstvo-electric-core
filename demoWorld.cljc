(ns krestianstvo.demoWorld
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [krestianstvo.krestianstvo :as k]
            [krestianstvo.objects :as o]
            [krestianstvo.painter :as p]))

(e/defn DemoWorld [seloID clientID worldsDir]
  (e/client
    (let [!objData (atom {})
          virtualTime (k/Selo. seloID clientID !objData)]

      (dom/div
        (dom/props {:style {:border-style "dotted"
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
            (dom/div
              (p/Painter. :painterObj seloID !objData virtualTime)

              (dom/div
                (dom/props {:style {:display "flex"
                                    :align-items "left"
                                    :justify-content "space-around"}})

                (o/Clicker. :clickerObj seloID !objData virtualTime)
                (o/Counter. :counterObj seloID !objData virtualTime)))

            (dom/div
              (o/Portal. :demoPortalObj seloID !objData virtualTime worldsDir))))))))

