(ns kaocha-nrepl.kaocha.midje
  (:require
   [kaocha-nrepl.kaocha.testable :as t]))

(defn- parse-testable
  [testable]
  (let [testable-meta (:kaocha.testable/meta testable)
        test-name (str (:midje/name testable-meta))]
    (for [fail (:kaocha.type.midje/midje-failures testable)]
      {:ns-name (str (:midje/namespace testable-meta))
       :test-name test-name
       :result {:type "fail"
                :line (-> fail :position second)
                :context ""
                :expected (-> fail :expected-result str)
                :actual (-> fail :actual str)
                :file (:midje/file testable-meta)
                :var test-name}})))

(defmethod t/errors :kaocha.type/midje
  [testable]
  (->> testable
       :kaocha.result/tests
       (mapcat :kaocha.result/tests)
       (mapcat parse-testable)))

(defmethod t/testing-ns :kaocha.type/midje
  [testable]
  (->> (:kaocha.result/tests testable)
       (remove :kaocha.testable/skip)
       (filter #(= :kaocha.type.midje/ns (:kaocha.testable/type %)))
       (map (comp str :kaocha.type.midje/ns-name))))
