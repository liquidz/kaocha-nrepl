(ns kaocha-nrepl.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [kaocha.plugin :as p]
            [kaocha.repl :as kaocha]
            [kaocha.result :as result]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]))

(def ^:dynamic *msg* nil)

(def current-report (atom nil))

(defn report-reset! []
  (reset! current-report {:summary {:ns 0 :var 0 :test 0 :pass 0 :fail 0 :error 0}
                          :results {}
                          :testing-ns nil
                          ;;:gen-input nil
                          }))

(defn- testing-var-names [test-plan]
  (->> test-plan
       :kaocha.test-plan/tests
       (mapcat :kaocha.test-plan/tests)
       (mapcat :kaocha.test-plan/tests)
       (map :kaocha.var/name)))

(comment
  {:results {:ns-name {:test-name [
                                   {:type "fail"
                                    :file ""
                                    :line 123
                                    :expected ""
                                    :actual ""
                                    :context ""
                                    :var ""
                                    :error ""
                                    :diffs []
                                    }
                                   ]}}}
  )

(defn- send! [msg m]
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
                       res (-> (select-keys m [:type :line :testing-contexts :expected :actual])
                               (set/rename-keys {:testing-contexts :context})
                               (update :context #(str/join " " %))
                               (update :type name)
                               (assoc :file (:file testable-meta)
                                      :var (str (:kaocha.var/name testable))))]
                   (update-in acc [ns-name test-name]
                              #(conj (or % []) res)))) {})))

(p/defplugin kaocha-nrepl/plugin
  (pre-test [test test-plan]
            (when *msg*
              (send! *msg* {:out (format "Testing: %s"
                                         (str/join ", " (testing-var-names test-plan)))}))
            test)
  (post-run [result]
            (when-let [tests (:kaocha.result/tests result)]
              (let [summary (-> tests result/totals result/totals->clojure-test-summary)]
                (->> tests
                     errors
                     ; (mapcat :kaocha.result/tests)
                     ; (mapcat :kaocha.result/tests)
                     ; (remove :kaocha.testable/skip)
                     ; (mapcat :kaocha.testable/events)
                     ; (filter #(#{:fail} (:type %)))
                     ; (map #(dissoc % :kaocha/test-plan))
                     clojure.pprint/pprint)))

            result))

(comment
  (def x (kaocha/run 'kaocha-nrepl.core-test/dummy-test
                     kaocha-config)))

(def ^:private kaocha-config
  {:kaocha/plugins [:kaocha-nrepl/plugin]})

(defn- test-ns-reply [msg]
  (binding [*msg* msg]
    (let [ns-sym (symbol (:ns msg))
          result (->> (kaocha/run ns-sym kaocha-config)
                      (map (fn [[k v]] [(name k) v]))
                      (into {}))]
      (send! msg (merge result {:status :done})))))

(defn wrap-kaocha [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      ;;"kaocha-test-all" nil
      "kaocha-test-ns" (test-ns-reply msg)
      ;;"kaocha-test" nil
      (handler msg)
      )))

(when (resolve 'set-descriptor!)
  (set-descriptor!
   #'wrap-kaocha
   {:doc "Sample nREPL wrapper"
    :requires #{} ; descriptors required by this descriptor
    :handles {"kaocha-test-all"
              {:doc "Sample nREPL middleware"
               :requires {}
               ;;:returns {"hello" "world" "status" "done"}
               }

              "kaocha-test-ns"
              {:doc "Sample nREPL middleware"
               :requires {"ns" "test target namespace"}
               ;;:returns {"hello" "world" "status" "done"}
               }

              "kaocha-test"
              {:doc "Sample nREPL middleware"
               :requires {}
               ;;:returns {"hello" "world" "status" "done"}
               }
              }}))
