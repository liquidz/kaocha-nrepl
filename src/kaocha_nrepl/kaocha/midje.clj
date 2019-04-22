(ns kaocha-nrepl.kaocha.midje
  (:require [clojure.string :as str]
            [kaocha-nrepl.kaocha.testable :as t]))

(defn- parse-fact [m]
  (let [testable (:kaocha/testable m)
        testable-meta (:kaocha.testable/meta testable)
        test-name (str (:midje/name testable-meta))]
    {:ns-name (str (:midje/namespace testable-meta))
     :test-name test-name
     :result {:type (-> m :type name)
              :line (:midje/line testable-meta)
              :context (->> m :testing-contexts (str/join " "))
              :expected (-> m :expected str)
              :actual (-> m :actual str)
              :file (:midje/file testable-meta)
              :var test-name}}))

(defmethod t/errors :kaocha.type/midje
  [testable]
  (->> (:kaocha.result/tests testable)
       (mapcat :kaocha.result/tests)
       (remove :kaocha.testable/skip)
       (mapcat :kaocha.testable/events)
       (filter #(#{:fail} (:type %)))
       (map parse-fact)))

(defmethod t/testing-ns :kaocha.type/midje
  [testable]
  (->> (:kaocha.result/tests testable)
       (remove :kaocha.testable/skip)
       (filter #(= :kaocha.type.midje/ns (:kaocha.testable/type %)))
       (map (comp str :kaocha.type.midje/ns-name))))
