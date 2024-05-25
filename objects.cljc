(ns krestianstvo.objects
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [missionary.core :as m]
            [contrib.electric-contrib :as ex]
            [krestianstvo.krestianstvo :as k]
            [clojure.string :as str]))

(defn getRandomColor [rng]
  #?(:cljs (str "#" (.toString (.floor js/Math (* rng 16777215)) 16))))

(e/defn Metronome [objID seloID !objData virtualTime]
  (e/client
    (let [objData (e/watch !objData)
          [timeNow msgRes sendFutureMsg prng] virtualTime]

      (let [tick  (fn [m]
                   ;; (println "Tick " m)
                    )]
        (if (or
              (= :all (:objid (:value msgRes)))
              (= objID (:objid (:value msgRes))))

          (let [msgName (:name (:value msgRes))]

            (case msgName
              :tick nil;;(k/doAction. (tick msgRes))
              "default"))))

      (e/on-unmount
        #(swap! !objData update-in [seloID] dissoc objID)))))

(e/defn Clicker [objID seloID !objData virtualTime]
  (e/client
    (let [objData (e/watch !objData)
          [timeNow msgRes sendFutureMsg prng] virtualTime
          color (e/client (-> objData seloID objID :color))

          randomColor (fn [m]
                        (swap! !objData assoc-in [seloID objID :color] (getRandomColor (prng))))

          initialize (fn [m]
                       (println "Initialize " m)
                       (swap! !objData assoc-in [seloID objID :color] (getRandomColor (prng)))
                       (swap! !objData assoc-in [seloID objID :initialised] true))]

      (if (or
            (= :all (:objid (:value msgRes)))
            (= objID (:objid (:value msgRes))))

        (let [msgName (:name (:value msgRes))]
          (case msgName
            :initialize (initialize msgRes)
            :randomColor (randomColor msgRes)
            "default")))

      (if (not (some? (:initialised (-> objData seloID objID))))
        (ex/after 500 (k/sendExtMsg. {:name :initialize, :objid objID} seloID nil)))

      (dom/div
        (dom/props {:style {:display "grid"
                            :align-items "center"
                            :justify-content "space-around"
                            :border-style "dotted"
                            :border-width "1px"
                            :padding "5px"}})

        (dom/div
          (dom/props {:style {:background color
                              :opacity 0.6
                              :width "125px"
                              :height "100px"}})

          (dom/on "click" (e/fn [e]
                            (k/sendExtMsg. {:name :randomColor, :objid objID} seloID nil))))))

    (e/on-unmount
      #(swap! !objData update-in [seloID] dissoc objID))))

(e/defn Counter [objID seloID !objData virtualTime]
  (e/client
    (let [objData (e/watch !objData)
          [timeNow msgRes sendFutureMsg prng] virtualTime
          x (e/client (-> objData seloID objID :x))]

      (let [;; tick  (fn [m]
            ;;         ;;(println "Tick " m)
            ;;         )

            increment (fn [m]
                        (println "M " m)
                        (swap! !objData update-in [seloID objID :x] inc))

            decrement (fn [m]
                        (swap! !objData update-in [seloID objID :x] dec))

            step (fn [m]
                   (swap! !objData update-in [seloID objID :x] inc)
                   (sendFutureMsg {:name :step, :when 0.5, :objid objID}))

            init (fn [m]
                   (println "INIT " m)
                   (swap! !objData assoc-in [seloID objID :x] 0)
                   (swap! !objData assoc-in [seloID objID :initialised] true))]
;;(println "MSR " msgRes)
        (when (or
                (= :all (:objid (:value msgRes)))
                (= objID (:objid (:value msgRes))))

          (let [msgName (:name (:value msgRes))]

            (case msgName
              ;;:tick (k/doAction. (tick msgRes))
              :inc  (increment msgRes)
              :dec (decrement msgRes)
              :step (step msgRes)
              :init (init msgRes)
              "default")))

        (if (not (some? (:initialised (-> objData seloID objID))))
          (ex/after 500 (k/sendExtMsg. {:name :init, :objid objID} seloID nil))))

      (dom/div
        (dom/props {:style {:display "grid"
                            :align-items "center"
                            :justify-content "space-around"
                            :border-style "dotted"
                            :border-width "1px"
                            :padding "5px"}})

        (dom/div
          (dom/props {:style {:display "flex"
                              :align-items "center"
                              :justify-content "space-around"
                              :width "150px"}})
          (dom/button
            (dom/text "-")
            (dom/on "click"
              (e/fn [e]
                (k/sendExtMsg. {:name :dec, :objid objID} seloID nil))))

          (dom/h2
            (dom/text x))

          (dom/button
            (dom/text "+")
            (dom/on "click" (e/fn [e]
                              (k/sendExtMsg. {:name :inc, :objid objID} seloID nil)))))

        (dom/div
          (dom/props {:style {:margin "auto"
                              :width "fit-content"}})

          (dom/button
            (dom/text "Loop")
            (dom/on "click" (e/fn [e]
                              (k/sendExtMsg. {:name :step, :objid objID} seloID nil))))))

      (e/on-unmount
        #(swap! !objData update-in [seloID] dissoc objID)))))

(e/defn Avatar [userID seloID me !objData virtualTime]
  (e/client
    (let [objID (keyword userID)
          objData (e/watch !objData)

          [timeNow msgRes sendFutureMsg prng] virtualTime
          mouse (e/client (:mouse (objID (get objData seloID))))

        ;;[x y] [(.-clientX e/dom-mousemove)
        ;;       (.-clientY e/dom-mousemove)]

          ;; TODO: local tick 
          ;; nowT (fn [] #?(:cljs (.now js/Date)))
          ;; clock (m/ap
          ;;         (let [timestamp (m/?> (m/seed (iterate (fn [previous] (+ previous 50)) (nowT))))]
          ;;           (m/? (m/sleep (- timestamp (nowT))))))
          ;; tv (e/fn [] (new (m/reductions + 0 (m/sample inc clock))))

          !aa (atom [0 0 0])
          aa (e/watch !aa)

          mflow (e/fn [] {:msg :mousemove
                          :value aa})

          throttle (fn [dur >in]
                     (m/ap
                       (let [x (m/?> (m/relieve {} >in))]
                         (m/amb x (do (m/? (m/sleep dur)) (m/amb))))))

          ;;mt (e/client (:throttle ((keyword me) (get objData seloID))))
          mc (e/fn []
               (new (m/reductions {} nil
                      (throttle 25 (m/eduction (dedupe) mflow)))))]

      ;; (if (not (some? mt))
      ;;   (swap! !objData assoc-in [seloID (keyword me) :throttle] 50))

      (let [init (fn [m]
                   (println "INIT " m)
                   (swap! !objData assoc-in [seloID objID :mouse :color] (getRandomColor (prng)))
                   (swap! !objData assoc-in [seloID objID :initialised] true))

            mouseMove (fn [m]
                        (let [coords (:xy (:data (:value msgRes)))]

                          (if (not-empty coords)
                            (swap! !objData assoc-in [seloID objID :mouse :coords]
                              {:x (first coords),
                               :y (second coords)}))))
            ;; tick  (fn [m]
            ;;         (println "Tick " m)
            ;;         )
            ]

        (if (or
              (= :all (:objid (:value msgRes)))
              (= objID (:objid (:value msgRes))))

          (let [msgName (:name (:value msgRes))]
            (case msgName
              ;;:tick (k/doAction. (tick msgRes))
              :init (init msgRes)
              :mousemove (mouseMove msgRes)
              "default"))))

      (if (= userID me)
        (do
          (if (not (some? (:initialised (-> objData seloID objID))))
            (ex/after 500 (k/sendExtMsg. {:name :init, :objid objID} seloID (keyword me))))

          (let [st (e/server  e/system-time-ms)]
            (dom/on! dom/node "mousemove"
              (fn [e]
                (let [bounds (. dom/node getBoundingClientRect)
                      scaleX (/ (. dom/node -offsetWidth) (. bounds -width))
                      scaleY (/ (. dom/node -offsetHeight) (. bounds -height))
                      x (int (* (- (.-clientX e) (. bounds -left) 0) scaleX))
                      y (int (* (- (.-clientY e) (+ (. bounds -top) (. dom/node -scrollTop))) scaleY))]
                  (reset! !aa [x y st])
                 ;; (reset! !aa [(.-clientX e) (.-clientY e)])
                  ))))

          (let [d (:value (mc.))
                ;; st (k/serverFunction
                ;;      (e/fn [c]
                ;;        (new (m/reductions {} nil
                ;;               (m/eduction
                ;;                 (map #(do
                ;;                         {:coords (:value %)
                ;;                          :instant (System/currentTimeMillis)}))
                ;;                 c))))
                ;;      (e/fn [] (e/fn []
                ;;                 (e/client (mc.)))))

;; TODO: local tick 
                ;; metro (k/serverFunction
                ;;         (e/fn [c]
                ;;           (new (m/reductions {} nil
                ;;                  (m/eduction
                ;;                    (map #(do
                ;;                            {:coords %
                ;;                             :instant (System/currentTimeMillis)}))
                ;;                    c))))
                ;;         (e/fn [] (e/fn []
                ;;                    (e/client (tv.)))))
                ]

            (k/sendExtMsg. {:name :mousemove, :objid objID,
                            :data {:xy d}} seloID (keyword me) (nth d 2))
            ;; (sendExtMsg. {:name :mousemove, :objid objID,
            ;;               :data {:xy (:coords st)}} (:instant st))

            ;; TODO: local tick
            ;; (if (= me (e/server (seloID k/master)))
            ;;   (do 
            ;;     ;;(println "Clock: " (tv.))
            ;;     (sendExtMsg. {:name :metro, :objid objID,
            ;;                   :data {:t (:coords metro)}} (:instant metro))
            ;;     )
            ;;   ) 
            )))

      (dom/div
        (dom/style {:opacity "0.7"
                    :border-style "solid"
                    :border-width "1px"
                    :border-color "black"
                    :width "20px"
                    :height "20px"
                    :border-radius "50%"
                    :position "absolute"
                    :font-size "small"
                    :transform (str "translate3d(" (str (- (-> mouse :coords :x) 10) "px") ", " (str (- (-> mouse :coords :y) 10) "px") ", 0px)")
                      ;; :transition "transform 0.1s cubic-bezier(.02,1.23,.79,1.08)"
                    :z-index 100
                    :pointer-events "none"
                    :top 0
                    :left 0
                    :background (-> mouse :color)})

        (dom/div (dom/style {:position "relative"
                             :left "40px"
                             :user-select "none"})
          (dom/text (subs userID 0 4) "..")
          (dom/br)
          (dom/div (dom/style {:width "100px"})
            (dom/b (dom/text (-> mouse :coords :x) " - " (-> mouse :coords :y))))))

      (e/server
        (e/on-unmount
          #(swap! k/!data update-in [seloID :obj] dissoc objID)))

      (e/on-unmount
        #(swap! !objData update-in [seloID] dissoc objID)))))

(e/defn Portal [objID seloID !objData virtualTime worlds]
  (e/client
    (let
      [objData (e/watch !objData)
       [timeNow msgRes sendFutureMsg prng] virtualTime
       pid (:portalID (objID (seloID objData)))

       setPortalID (fn [m]
                     (println "msg: " m)
                     (swap! !objData assoc-in [seloID objID :portalID] (:portalID (:data (:value m)))))]

;; (e/server
      ;;   (if (not (some? (objID (:obj (seloID k/data)))))
      ;;     (swap! k/!data assoc-in [seloID :obj objID] {})))

      (if (= objID (:objid (:value msgRes)))

        (let [msgName (:name (:value msgRes))]
          (case msgName
            :setPortalID (setPortalID msgRes)
            "default")))

      (dom/div (dom/props {:style {:border-style "dotted"
                                   :border-width "1px"
                                   :padding "5px"
                                   :position "relative"}})
        (dom/input
          (dom/props {:placeholder "Portal (enter seloID)" :maxlength 100})

          (dom/on "keydown" (e/fn [e]
                              (when (= "Enter" (.-key e))
                                (when-some [v (.. e -target -value)]
                                  (k/sendExtMsg. {:name :setPortalID, :data {:portalID ""}, :objid objID} seloID nil)
                                  (ex/after 500
                                    (k/sendExtMsg. {:name :setPortalID, :data {:portalID v}, :objid objID} seloID nil))))))

          (if (not-empty pid)
            (set! (.-value dom/node) pid)))

        (if (not-empty pid)
          (let [[worldName world] (str/split pid #":" 2)]
            (if (or (= "demo" world) (empty? world) (not (some? (get worlds (keyword world)))))
              (new (get worlds :demo) (keyword worldName) (str (random-uuid)) worlds)
              (new (get worlds (keyword world)) (keyword worldName) (str (random-uuid))))))))
    (e/server
      (e/on-unmount
        #(swap! k/!data update-in [seloID :obj] dissoc objID)))))

(e/defn Header [seloID clientID virtualTime !objData]
  (e/client
    (let [[timeNow msgRes] virtualTime
          objData (e/watch !objData)]
      (dom/div
        (dom/props {:style {:width "200px"}})
        (dom/b (dom/text
                 "ID " seloID
                 (if (= clientID (e/server (seloID k/master))) " (master)")))
        (dom/br)
        ;; TODO: Mouse throttle
        ;; (let [mt (e/client (:throttle ((keyword clientID) (get objData seloID))))] 
        ;;   (if (some? mt)
        ;;   (dom/div
        ;;     (dom/span
        ;;       (dom/props {:style {:padding-right "10px"}})
        ;;       (dom/text "Mouse throttle"))
        ;;       (dom/select
        ;;         (e/for [id [25 50 100 200 500 1000]]
        ;;           (dom/option
        ;;             (dom/props {:value (str id) :selected (= mt id)})
        ;;             (dom/text (str id))))
        ;;         (dom/on "change" (e/fn [^js e]
        ;;                            (swap! !objData assoc-in [seloID (keyword clientID) :throttle] (int (.. e -target -value)))))))))
        (dom/text "Time Now: ")
        (dom/b (dom/text (k/vTimeInSec. seloID timeNow)))))
    (dom/hr)))

(e/defn SeloInfo [seloID clientID !objData virtualTime]
  (e/client
    (let [objData (e/watch !objData)
          [timeNow msgRes sendFutureMsg prng] virtualTime]
      (dom/div
        (dom/text "Selo ID  " seloID)
        (dom/br)
        (dom/text "Me: " clientID)
        (dom/b (dom/text
                 (if (= clientID (e/server (seloID k/master)))
                   " (I AM MASTER)")))
        (dom/br)
        (dom/text "Time Now: " (k/vTimeInSec. seloID timeNow))
        (dom/br)
        (dom/text "All Users: " (e/server k/users))
        (dom/br)
        (dom/text "All Masters: " (e/server k/master))
        (dom/br)
        (dom/div
          (dom/text "Clients:")
          (dom/ul
            (e/for [e  (e/server (:clients (seloID k/data)))]
              (dom/li (dom/text e)))))
        ;;  (dom/div (dom/text "Current msg: " msgRes))
        (dom/b (dom/text "Data on CLIENT: "))
        (dom/ul
          (e/for [[k v] (seloID objData)]
            (dom/li (dom/text k " - " v))))

        (dom/b (dom/text "Data on SERVER: "))
        (dom/ul
          (e/for [[k v] (e/server (seloID k/data))]
            (if (not= :obj k)
              (dom/li
                (dom/text k " - " v)))))
        (dom/text "Object msg flows: ")
        (dom/ul
          (e/for [[k v] (e/server (:obj (seloID k/data)))]
            (dom/li
              (dom/text k " - " v))))
        (dom/text "Last server snapshot on sync: ")
        (dom/ul
          (e/for [[k v] (e/server (seloID k/stateQ))]
            (dom/li (dom/text k " - " v))))))))