(ns kaocha-nrepl.kaocha
  (:require [clojure.string :as str]
            [kaocha-nrepl.kaocha.testable :as testable]
            [kaocha.plugin :as p]
            [kaocha.repl :as kaocha]
            [kaocha.result :as result]))

(require 'kaocha-nrepl.kaocha.midje)

(def current-report (atom nil))
(def last-context (atom nil))

(defn reset-report! []
  (reset! current-report
          {:summary {:ns 0 :var 0 :test 0 :pass 0 :fail 0 :error 0}
           :results {}
           :testing-ns nil}))

(defn reset-last-context!
  ([] (reset-last-context! {}))
  ([config]
   (reset! last-context
           {:config config
            :failed-testable-ids []})))

(defn errors [testables]
  (->> testables
       (mapcat testable/errors)
       (reduce (fn [acc {:keys [ns-name test-name result]}]
                 (update-in acc [ns-name test-name] #(conj (or % []) result)))
               {})))

(defn totals [testable]
  (let [res (-> testable
                result/totals
                result/totals->clojure-test-summary)
        ns-count (->> testable
                      (mapcat testable/testing-ns)
                      count)]
    (-> res
        (assoc :var (:test res)
               :test (+ (:pass res) (:fail res) (:error res))
               :ns ns-count)
        (select-keys [:ns :var :test :pass :fail :error]))))

(defn testing-ns [testable]
  (->> testable
       (mapcat testable/testing-ns)
       (str/join ", ")))

(p/defplugin kaocha-nrepl/plugin
  (post-run
   [result]
   (when-let [testable (:kaocha.result/tests result)]
     (reset! current-report
             {:results (errors testable)
              :summary (totals testable)
              :testing-ns (testing-ns testable)}))
   result))
(defn failed-testable-ids [testable]
    (->> testable
         (mapcat :kaocha.result/tests)
         (mapcat :kaocha.result/tests)
         (filter result/failed?)
         (map :kaocha.testable/id)))

(def ^:private default-kaocha-config
  {:kaocha/plugins [:kaocha-nrepl/plugin]
   :kaocha/reporter []})

(defn- gen-args [args]
  (let [[config args] (if (map? (last args))
                        ((juxt last butlast) args)
                        [{} args])
        config (merge default-kaocha-config config)]
    (concat args [config])))

(defn run [& args]
  (reset-report!)
  (let [args (gen-args args)]
    (reset-last-context! (last args))
    (doall (apply kaocha/run args)))
  @current-report)

(defn rerun []
  (when-let [ids (:failed-testable-ids @last-context)]
    (reset-report!)
    (let [args (concat ids [(:config @last-context)])]
      (doall (apply kaocha/run args))
      @current-report)))

(defn run-all [config]
  (reset-report!)
  (let [config (-> [config] gen-args first)]
    (reset-last-context! config)
    (kaocha/run-all config))
  @current-report)
