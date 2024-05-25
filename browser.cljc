(ns krestianstvo.browser
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [missionary.core :as m]
            [krestianstvo.demoWorld :as dw]
            [krestianstvo.worlds :as w]
            [clojure.string :as str]))

(e/defn Browser []
  (e/client
    (let [!seloID (atom "")
          seloID (e/client (e/watch !seloID))
          clientID (e/server (get-in e/http-request [:headers "sec-websocket-key"]))]

      (dom/div
        (dom/a (dom/props {:href "/(krestianstvo.browser!Browser)" :target "_blank"}) (dom/text "Link to world"))
        (dom/div
          (dom/props {:style {:margin "auto"
                              :width "fit-content"}})
          (dom/h4
            (dom/text "Krestianstvo | Electric Clojure")))

        (dom/div
          (dom/props {:style {:display "grid"
                              :align-items "center"
                              :justify-content "space-around"}})

          (dom/div
            (dom/props {:style {:padding "10px"}})
            (dom/text "Example url queries: | 1 | a:counter | 2:painter | c:clicker | 1:portal | d:demo"))

          (dom/input
            (dom/props {:placeholder "Enter seloID or seloID:world" :maxlength 200})
            (dom/on "keydown" (e/fn [e]
                                (when (= "Enter" (.-key e))
                                  (when-some [v (.. e -target -value)]
                                    (new (e/task->cp (m/sp
                                                       (reset! !seloID "")
                                                       (m/? (m/sleep 0))
                                                       (reset! !seloID v)))))))))
          (dom/div
            (dom/props {:style {:display "grid"
                                :align-items "center"
                                :justify-content "space-around"}})

            (when (not-empty seloID)
              (let [[worldName world] (str/split seloID #":" 2)]
                (if (or (= "demo" world) (empty? world) (not (some? (get w/worldsDir (keyword world)))))
                  (dw/DemoWorld. (keyword worldName) clientID w/worldsDir)
                  (new (get w/worldsDir (keyword world)) (keyword worldName) clientID))))))))))