(ns integration-test
  (:require [clojure.string :as str]
            [kaocha-nrepl.kaocha :as kaocha]
            [kaocha-nrepl.test-helper :as h]))

(defn- ensure-map [x]
  (cond
    (sequential? x) (apply merge x)
    :else x))

(defn run-clojure-test-test []
  (h/with-test-server [session]
    (let [id (h/random-id)
          resp (-> session
                   (h/send-message {:id id :op "kaocha-test" :testable-ids ["unit"] :config-file "integration/clojure_test.edn"})
                   ensure-map)]
      (assert (= ["done"] (:status resp)))
      (assert (= id (:id resp)))
      (assert (= {:ns 2 :var 4 :test 6 :pass 3 :fail 3 :error 0}
                 (:summary resp)))
      (assert (= ["cljtest.fail-test" "cljtest.success-test"]
                 (-> (:testing-ns resp) (str/split  #"\s*,\s*")
                     sort)))
      (assert (= {:cljtest.fail-test
                  {:test3 [{:actual "false"
                            :context ""
                            :expected "false"
                            :file "cljtest/fail_test.clj"
                            :line 5
                            :type "fail"
                            :var "cljtest.fail-test/test3"}]
                   :test4 [{:actual "(not (= \"foo\" \"bar\"))"
                            :context "context4"
                            :expected "(= \"foo\" \"bar\")"
                            :file "cljtest/fail_test.clj"
                            :line 9
                            :type "fail"
                            :var "cljtest.fail-test/test4"}
                           {:actual "(not (= \"bar\" \"baz\"))"
                            :context "context4"
                            :expected "(= \"bar\" \"baz\")"
                            :file "cljtest/fail_test.clj"
                            :line 10
                            :type "fail"
                            :var "cljtest.fail-test/test4"}]}}
                 (:results resp))))))

(defn- run-midje-test []
  (h/with-test-server [session]
    (let [id (h/random-id)
          resp (-> session
                   (h/send-message {:id id :op "kaocha-test" :testable-ids ["unit"] :config-file "integration/midje.edn"})
                   ensure-map)]
      (assert (= ["done"] (:status resp)))
      (assert (= id (:id resp)))
      (assert (= {:ns 2 :var 4 :test 6 :pass 3 :fail 3 :error 0} (:summary resp)))
      (assert (= ["midjetest.fail-test" "midjetest.success-test"]
                 (-> (:testing-ns resp) (str/split  #"\s*,\s*")
                     sort)))
      (assert (= {:midjetest.fail-test
                  {:context4
                   [{:actual "foo"
                     :context ""
                     :expected "bar"
                     :file "midjetest/fail_test.clj"
                     :line 11
                     :type "fail"
                     :var "context4"}
                    {:actual "bar"
                     :context ""
                     :expected "baz"
                     :file "midjetest/fail_test.clj"
                     :line 12
                     :type "fail"
                     :var "context4"}]
                   :test3
                   [{:actual "true"
                     :context ""
                     :expected "false"
                     :file "midjetest/fail_test.clj"
                     :line 8
                     :type "fail"
                     :var "test3"}]}}
                 (:results resp))))))

(defn run-retest-test []
  (h/with-test-server [session]
    (doall (h/send-message session {:op "kaocha-test" :testable-ids ["unit"] :config-file "integration/clojure_test.edn"}))

    (assert (= "integration/clojure_test.edn"
               (get-in @kaocha/last-context [:config :config-file])))
    (assert (= #{:cljtest.fail-test/test3 :cljtest.fail-test/test4}
               (set (:failed-testable-ids @kaocha/last-context))))

    (let [id (h/random-id)
          resp (-> session
                   (h/send-message {:id id :op "kaocha-retest"})
                   ensure-map)]
      (assert (= ["done"] (:status resp)))
      (assert (= id (:id resp)))
      (assert (= {:ns 1 :var 2 :test 3 :pass 0 :fail 3 :error 0}
                 (:summary resp)))
      (assert (= "cljtest.fail-test" (:testing-ns resp)))
      (assert (= {:cljtest.fail-test
                  {:test3 [{:actual "false"
                            :context ""
                            :expected "false"
                            :file "cljtest/fail_test.clj"
                            :line 5
                            :type "fail"
                            :var "cljtest.fail-test/test3"}]
                   :test4 [{:actual "(not (= \"foo\" \"bar\"))"
                            :context "context4"
                            :expected "(= \"foo\" \"bar\")"
                            :file "cljtest/fail_test.clj"
                            :line 9
                            :type "fail"
                            :var "cljtest.fail-test/test4"}
                           {:actual "(not (= \"bar\" \"baz\"))"
                            :context "context4"
                            :expected "(= \"bar\" \"baz\")"
                            :file "cljtest/fail_test.clj"
                            :line 10
                            :type "fail"
                            :var "cljtest.fail-test/test4"}]}}
                 (:results resp))))))

(defn -main []
  (println "\nIntegration tests: start >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
  (run-clojure-test-test)
  (run-midje-test)
  (run-retest-test)
  (println "Integration tests: end   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n")
  (System/exit 0))
