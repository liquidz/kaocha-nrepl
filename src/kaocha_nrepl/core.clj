(ns kaocha-nrepl.core
  (:require [clojure.string :as str]
            [kaocha.plugin :as p]
            [kaocha.repl :as kaocha]
            [kaocha.result :as result]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]))

(def ^:dynamic *msg* nil)

(def current-report (atom nil))

(defn reset-report! []
  (reset! current-report
          {:summary {:ns 0 :var 0 :test 0 :pass 0 :fail 0 :error 0}
           :results {}
           :testing-ns nil}))

(defn- send! [m msg]
  (transport/send (:transport msg) (response-for msg m)))

(defn errors [testables]
  (->> testables
       (mapcat :kaocha.result/tests)
       (mapcat :kaocha.result/tests)
       (remove :kaocha.testable/skip)
       (mapcat :kaocha.testable/events)
       (filter #(#{:fail} (:type %)))
       (reduce (fn [acc m]
                 (let [testable (:kaocha/testable m)
                       testable-meta (:kaocha.testable/meta testable)
                       ns-name (str (:ns testable-meta))
                       test-name  (str (:name testable-meta))
                       res {:type (-> m :type name)
                            :line (:line m)
                            :context (->> m :testing-contexts (str/join " "))
                            :expected (-> m :expected str)
                            :actual (-> m :actual str)
                            :file (:file testable-meta)
                            :var (-> testable :kaocha.var/name str)}]
                   (update-in acc [ns-name test-name]
                              #(conj (or % []) res)))) {})))

(defn totals [testable]
  (let [res (-> testable
                result/totals
                result/totals->clojure-test-summary)
        ns-count (->> testable
                      (mapcat :kaocha.result/tests)
                      (remove :kaocha.testable/skip)
                      (filter #(= :kaocha.type/ns (:kaocha.testable/type %)))
                      count)]
    (-> res
        (assoc :var (+ (:pass res) (:fail res) (:error res))
               :ns ns-count)
        (select-keys [:ns :var :test :pass :fail :error]))))

(defn testing-ns [testable]
  (->> testable
       (mapcat :kaocha.result/tests)
       (remove :kaocha.testable/skip)
       (filter #(= :kaocha.type/ns (:kaocha.testable/type %)))
       (map (comp str :kaocha.ns/name))
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

(comment
  (kaocha/run 'kaocha-nrepl-dev.success-test
              'kaocha-nrepl-dev.fail-test
              (assoc default-kaocha-config
                     :config-file "dev/config.edn")))

(def ^:private default-kaocha-config
  {:kaocha/plugins [:kaocha-nrepl/plugin]
   :kaocha/reporter []})

(defn kaocha-run [& args]
  (reset-report!)
  (doall (apply kaocha/run args))
  @current-report)

(defn- test-ns-reply [msg]
  (binding [*msg* msg]
    (let [{:keys [config-file]} msg
          ns-sym (symbol (:ns msg))
          config (cond-> default-kaocha-config
                   config-file (assoc :config-file config-file))]
      (-> (kaocha-run ns-sym config)
          (merge {:status :done})
          (send! msg)))))

(defn wrap-kaocha [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      ;;"kaocha-test-all" nil
      "kaocha-test-ns" (test-ns-reply msg)
      ;;"kaocha-test" nil
      (handler msg))))

(when (resolve 'set-descriptor!)
  (set-descriptor!
   #'wrap-kaocha
   {:doc "Sample nREPL wrapper"
    :requires #{} ; descriptors required by this descriptor
    :handles {"kaocha-test-all"
              {:doc "Sample nREPL middleware"
               :requires {}}

              "kaocha-test-ns"
              {:doc "Sample nREPL middleware"
               :requires {"ns" "test target namespace"}
               :optional {"config-file" "kaocha configuration file"}}

              "kaocha-test"
              {:doc "Sample nREPL middleware"
               :requires {}}}}))
