(ns krestianstvo.krestianstvo
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [missionary.core :as m]
            [contrib.missionary-contrib :as mx]
            [contrib.electric-contrib :as ex]
            #?(:cljs ["alea" :as Alea]))
  #?(:cljs (:require-macros [krestianstvo.krestianstvo :refer [serverFunction]])))

#?(:clj (defonce !master (atom {})))
(e/def master (e/server (e/watch !master)))

#?(:clj (def !users (atom {})))
(e/def users (e/server (e/watch !users)))

#?(:clj (def !data (atom {:selo {:tick {}, :obj {}, :starttime 0, :clients #{}}})))
(e/def data (e/server (e/watch !data)))

#?(:clj (defn -get-Time [_] (System/currentTimeMillis)))

#?(:clj (def !getState (atom {})))
(e/def getState (e/server (e/watch !getState)))

#?(:clj (def !stateQ (atom {})))
(e/def stateQ (e/server (e/watch !stateQ)))

(defmacro serverFunction [F a]
  `(e/server
     (new ~F (new ~a))))

(e/defn vTimeInSec [seloID time] (double (/ (- time (e/server (:starttime (seloID data)))) 1000)))

(e/defn sendExtMsg [msg seloID userID & ts]
  (e/server
    (swap! !data assoc-in [seloID :obj (:objid msg)]
      {:name  (:name msg),
       :data (:data msg),
       :objid (:objid msg),
       :when 0,
       :seloID seloID,
       :clientID userID,
       :origin :reflector,
       :timestamp (if (some? (first ts)) (first ts)
                    (e/snapshot e/system-time-ms))})))

;;(defn- qu [] #?(:clj clojure.lang.PersistentQueue/EMPTY :cljs #queue []))

(e/defn Selo [seloID userID !objData]
  (e/client
    (let [objData (e/watch !objData)

          master-user (e/server
                        (if (and (some? (get users (seloID master)))
                              (some? (get (:clients (seloID data)) (get master seloID))))
                          (seloID master)
                          (swap! !master assoc seloID userID)))

          ;Timestamped messages, that are inserted into queue are sorted
          queueSort (fn [i j]
                      (let [a (get-in i [:value])
                            b (get-in j [:value])]
                        (let [c (- (:timestamp a) (:timestamp b))]
                          (if (not= (:timestamp a) (:timestamp b))
                            c
                            (let [c (and (not= (:origin a) :reflector) (= (:origin b) :reflector))]
                              (if (= c true)
                                -1
                                (let [c (and (= (:origin a) :reflector) (not= (:origin b) :reflector))]
                                  (if (= c true)
                                    1
                                    (- (:seq a) (:seq b))))))))))

          !synced (atom false)
          synced (e/watch !synced)

          !inited (atom false)
          inited (e/watch !inited)

          !play (atom true)
          play (e/watch !play)

          !tick (atom true)
          tick  (e/watch !tick)

          !seq (atom 0)
          seq (e/watch !seq)

          prng (Alea.)

          mbxRes (m/mbx)
          msgRes  (new (m/reductions {} nil (mx/poll-task mbxRes)))

          mbx (m/mbx)
          preMsg  (new (m/reductions {} nil
                         (m/eduction
                           (map
                             #(do
                                {:value {:name (:name %),
                                         :when (:when %),
                                         :data (:data %),
                                         :objid (:objid %),
                                         :origin (:origin %),
                                         :seloID (:seloID %),
                                         :timestamp (:timestamp %),
                                         :clientID (:clientID %),
                                         :seq (swap! !seq inc)}}))
                           (mx/poll-task mbx))))

          !timeNow (atom 0)
          timeNow (e/watch !timeNow)

          !reflectorTime (atom 0)
          reflectorTime (e/watch !reflectorTime)

          !msgQueue (atom #{})
          msgQueue (e/watch !msgQueue)

          sendFutureMsg (fn [msg]
                          (let [fm (merge msg
                                     {:origin :future,
                                      :seloID seloID,
                                      :clientID userID,
                                      :timestamp (+ timeNow (int (* 1000 (:when msg))))})]
                            (mbx fm)
                            ;;(swap! !msgQueue conj ms)
                            ))

          sortedMsgQueue (e/fn [] (sort-by queueSort msgQueue))

          actQueue (fn [c]
                     (m/ap
                       (let [x (m/?> sortedMsgQueue)]
                         (if (and play (> (count x) 0))
                           (let [i (m/?> (m/seed x))
                                 ts (:timestamp (:value i))]
                             (if (<= ts c)
                               (m/amb
                                 (do
                                   (reset! !timeNow ts)
                                   (mbxRes i)
                                   (swap! !msgQueue disj i))
                                 (m/? (m/sleep 0))
                                 (m/?> (mbxRes nil))
                                 (m/amb))))))))

          dispatchFlow (e/fn []
                         (e/for [el (e/server (:obj (get data seloID)))]
                           (mbx (second el)))

                         (let [st (e/server e/system-time-ms)]
                           (if tick
                             (new (actQueue (reset! !reflectorTime st)))))

                         (when (and preMsg
                                 (:value preMsg))
                           (swap! !msgQueue conj preMsg)
                           (if (not tick)
                             (if (= :reflector (:origin (:value preMsg)))
                               (new (actQueue (reset! !reflectorTime (:timestamp (:value preMsg)))))))))

          ;; updateReflectorTime (e/fn []
          ;;                       (let [st (e/server e/system-time-ms)
          ;;                             tm {:name :tick,
          ;;                                 :objid :all,
          ;;                                 :seloID seloID
          ;;                                 :data {}
          ;;                                 :when 0,
          ;;                                 :origin :reflector,
          ;;                                 :clientID userID,
          ;;                                 :timestamp st}]
          ;;                         (mbx tm)))

;;route flow to internal msg queue
          ;;processFlow (e/fn [] (new (m/reductions {} nil (m/eduction (map #(mbx %)) multi)))) 
          ]
      ;;(println "Q " (sortedMsgQueue.)) 

      (dispatchFlow.)
      (if synced
        (when-let [prs (:seed (seloID objData))]
          (.importState prng (clj->js prs))))

    ; pause & play local queue
      (dom/div
        (let [lab (if play "pause View" "resume View")]
          (dom/button
            (dom/on! "click" (fn [e] (swap! !play not)))
            (dom/text lab))))

      (dom/div
        (let [lab (if tick "pause Tick" "resume Tick")]
          (dom/button
            (dom/on! "click" (fn [e] (swap! !tick not)))
            (dom/text lab))))

;;SYNC INITIAL CONNCECTIONS. TODO FIX.
      (e/server
        (when (= userID master-user)
          (e/client
            (reset! !synced true)
            (reset! !inited true))

          (when (seloID getState)

            (e/client
              (println "GET STATE!")
              (swap! !objData assoc-in [seloID :seed] (.exportState prng))
              (swap! !objData assoc-in [seloID seloID :queue] msgQueue))

            (swap! !getState assoc-in [seloID] false)
            (swap! !stateQ assoc-in [seloID] (get (e/client objData) seloID)))))

      (when (not= userID master-user)

        (println "Sync request! from: " master-user)
        (e/server (swap! !getState  assoc-in [seloID] true))

        (when (and synced (not inited))
          (println "RESTORE QUEUE!")
          (swap! !msgQueue into (-> objData seloID seloID :queue))
          (reset! !inited true))

        (when (not inited)
          (swap! !objData assoc-in [seloID] (e/server (seloID stateQ))))

        (e/server
          (when (and (not (seloID getState)) (not synced))
            (e/client (println "NEED TO SYNC!")
              (reset! !synced true)))))
      ;;END of Sync

      ;;Register User and create new Selo
      (e/server
        (if (not (some? (seloID (:obj (seloID data)))))
          (swap! !data assoc-in [seloID :obj seloID] {})))

      (let [startT (e/server (e/snapshot e/system-time-ms))]
        (e/server
          (swap! !users assoc userID {:id userID :data {}})
          (if (not (some? (seloID data)))
            (swap! !data assoc seloID
              {:obj {},
               :tick {},
               :starttime startT,
               :clients #{}})
            (swap! !data update-in [seloID :clients] conj userID))))

      (e/server
        (if (= (count users) 0)
          (do
            (println "Reset State!")
            (reset! !stateQ {})
            (reset! !data {})
            (reset! !master {})
            (swap! !master dissoc seloID)))

        (e/on-unmount #(swap! !data update-in [seloID :clients] disj userID))
            ;;(e/on-unmount #(swap! !master dissoc seloID))
        (e/on-unmount #(swap! !users dissoc userID)))

      [timeNow msgRes sendFutureMsg prng userID])))