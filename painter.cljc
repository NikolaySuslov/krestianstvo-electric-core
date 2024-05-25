(ns krestianstvo.painter
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [contrib.electric-contrib :as ex]
            [krestianstvo.krestianstvo :as k]
            [hyperfiddle.electric-svg :as svg]
            [clojure.string :as str]))

(e/defn Path [{:keys [points color]}]
  (e/client
    (svg/polyline
      (dom/props {:points (str/join " " (map #(str (first %) "," (second %)) points))
                  :stroke color
                  :fill "none"
                  :stroke-width "5"
                  :stroke-linecap "round"
                  :stroke-linejoin "round"
                  :opacity "0.9"}))))

(e/defn Toolbar [objID seloID userID]
  (e/client
    (dom/div
      (dom/style {:background "#fff5"
                  :backdrop-filter "blur(10px)"
                  :position "absolute"
                  :z-index "1"
                  :display "flex"
                  :top "10px"
                  :left "10px"
                  :border-radius "10px"
                  :box-shadow "0 0 5px rgba(0, 0, 0, 0.2)"
                  :flex-direction "column"
                  :justify-content "space-between"
                  :padding "10px"})

      (dom/div
        (e/for [color ["#0f172a" "#dc2626" "#ea580c"
                       "#fbbf24" "#a3e635" "#16a34a"
                       "#0ea5e9" "#4f46e5" "#c026d3"]]
          (dom/div
            (dom/style {:border-radius "100px"
                        :border "1px solid #eeea"
                        :width "5px"
                        :height "5px"
                        :padding "5px"
                        :margin-bottom "10px"
                        :background color})
            (dom/on "click"
              (e/fn [e]
                (k/sendExtMsg. {:name :setColor, :objid objID, :data {:color color}} seloID userID))))))

      (dom/div
        (dom/on "click"
          (e/fn [e]
            (k/sendExtMsg. {:name :resetCanvas, :objid objID} seloID userID)))
        (dom/text "🗑️")))))

(e/defn Painter [objID seloID !objData virtualTime]
  (e/client
    (let [[timeNow msgRes sendFutureMsg prng userID] virtualTime
          objData (e/watch !objData)

          ;;color (e/client (-> objData seloID objID :color))
          ;;myAvatar (get (seloID objData) (keyword clientID))
          paths (e/client (-> objData seloID objID :paths))

          !current-path-id (atom {:id :nil, :cursor nil})
          current-path-id (e/watch !current-path-id)

          setColor (fn [m]
                     (let [avatarID (keyword (:clientID (:value m)))]
                       (swap! !objData assoc-in [seloID avatarID :mouse :color] (:color (:data (:value m))))))

          initialize  (fn [m]
                        (println "Initialize " m)
                        (swap! !objData assoc-in [seloID objID :paths] (sorted-map))
                        (swap! !objData assoc-in [seloID objID :initialised] true))

          pointerDown (fn [m]
                        (println "Pointer Down: " m)
                        (let [av (get (seloID objData) (keyword (:clientID (:value m))))
                              penColor (:color (:mouse av))]
                          (reset! !current-path-id {:id (:id (:data (:value m))),
                                                    :cursor (:clientID (:value m))})
                          (swap! !objData assoc-in [seloID objID :paths (:id (:data (:value m)))]
                            {:points [[(:x (:coords (:mouse av))) (:y (:coords (:mouse av)))]]
                             :color penColor})))

          pointerUp (fn [m]
                      (println "Pointer Up: " m)
                      (reset! !current-path-id {:id :nil, :cursor (:clientID (:value m))}))

          resetCanvas (fn [m] (swap! !objData assoc-in [seloID objID :paths] (sorted-map)))]

      (if (or
            (= :all (:objid (:value msgRes)))
            (= objID (:objid (:value msgRes))))

        (let [msgName (:name (:value msgRes))]

          (case msgName
            :initialize (initialize msgRes)
            :setColor (setColor msgRes)
            :pointerdown (e/snapshot (pointerDown msgRes))
            :pointerup (pointerUp msgRes)
            :resetCanvas (resetCanvas msgRes)
            "default")))

      (if (not (some? (:initialised (-> objData seloID objID))))
        (ex/after 500 (k/sendExtMsg. {:name :initialize, :objid objID} seloID userID)))

      (let [canvas-size 300]
        (dom/div
          (dom/props {:style {:position "relative"}})

          (dom/on "pointerdown" (e/fn [e]
                                  (k/sendExtMsg. {:name :pointerdown, :objid objID,
                                                  :data {:id (.now js/Date)}} seloID userID)))

          (dom/on "pointerup" (e/fn [e]
                                (e/client
                                  (k/sendExtMsg. {:name :pointerup, :objid objID, :data {:id :nil}} seloID userID))))

          (svg/svg
            (dom/props {:viewBox (str "0 0 " canvas-size " " canvas-size)
                        :style {:position "relative"
                                :top "0"
                                :left "0"
                                :border "2px solid"
                                :pointer-events "none"
                                :width (str canvas-size "px")
                                :height (str canvas-size "px")}})
            (e/client
              (e/for-by key [[ke v] paths]
                (do
                  (Path. v))))

            (when (not= (:id current-path-id) :nil)
              (let [av (get (seloID objData) (keyword (:cursor current-path-id)))]
                (swap! !objData update-in [seloID objID :paths (:id current-path-id) :points] conj [(:x  (:coords (:mouse av))) (:y (:coords (:mouse av)))])))))

        (Toolbar. objID seloID userID))

      (e/on-unmount
        #(swap! !objData update-in [seloID] dissoc objID)))))


