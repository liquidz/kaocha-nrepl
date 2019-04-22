(ns kaocha-nrepl.kaocha.testable
  (:require [clojure.string :as str]))

(defmulti errors :kaocha.testable/type)
(defmulti testing-ns :kaocha.testable/type)

(defn- parse-test-var [m]
  (let [testable (:kaocha/testable m)
        testable-meta (:kaocha.testable/meta testable)]
    {:ns-name (str (:ns testable-meta))
     :test-name (str (:name testable-meta))
     :result {:type (-> m :type name)
              :line (:line m)
              :context (->> m :testing-contexts (str/join " "))
              :expected (-> m :expected str)
              :actual (-> m :actual str)
              :file (:file testable-meta)
              :var (-> testable :kaocha.var/name str)}}))

(defmethod errors :default
  [testable]
  (->> (:kaocha.result/tests testable)
       (mapcat :kaocha.result/tests)
       (remove :kaocha.testable/skip)
       (mapcat :kaocha.testable/events)
       (filter #(#{:fail} (:type %)))
       (map parse-test-var)))

(defmethod testing-ns :default
  [testable]
  (->> (:kaocha.result/tests testable)
       (remove :kaocha.testable/skip)
       (filter #(= :kaocha.type/ns (:kaocha.testable/type %)))
       (map (comp str :kaocha.ns/name))))
