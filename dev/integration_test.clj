(ns integration-test
  (:require
   [clojure.string :as str]
   [kaocha-nrepl.kaocha :as kaocha]
   [kaocha-nrepl.test-helper :as h]))

(defn- ensure-map
  [x]
  (cond
    (sequential? x) (apply merge x)
    :else x))

(defn run-clojure-test-test
  []
  (h/with-test-server [session]
    (let [id (h/random-id)
          resp (-> session
                   (h/send-message {:id id :op "kaocha-test" :testable-ids ["unit"] :config-file "integration/clojure_test.edn"})
                   ensure-map)]
      (assert (= ["done"] (:status resp)))
      (assert (= id (:id resp)))
      (assert (= {:ns 3 :var 5 :test 7 :pass 3 :fail 3 :error 1}
                 (:summary resp)))
      (assert (= ["cljtest.error-test" "cljtest.fail-test" "cljtest.success-test"]
                 (-> (:testing-ns resp) (str/split  #"\s*,\s*")
                     sort)))
      (assert (= {:test3 [{:actual "false"
                           :context ""
                           :expected "false"
                           :file "cljtest/fail_test.clj"
                           :line 6
                           :type "fail"
                           :var "cljtest.fail-test/test3"}]
                  :test4 [{:actual "(not (= \"foo\" \"bar\"))"
                           :context "context4"
                           :expected "(= \"foo\" \"bar\")"
                           :file "cljtest/fail_test.clj"
                           :line 10
                           :type "fail"
                           :var "cljtest.fail-test/test4"}
                          {:actual "(not (= \"bar\" \"baz\"))"
                           :context "context4"
                           :expected "(= \"bar\" \"baz\")"
                           :file "cljtest/fail_test.clj"
                           :line 11
                           :type "fail"
                           :var "cljtest.fail-test/test4"}]}
                 (get-in resp [:results :cljtest.fail-test])))

      (let [err-resp (get-in resp [:results :cljtest.error-test])
            test5 (some-> err-resp :test5 first)]
        (assert (= [:test5] (-> err-resp keys sort)))
        (assert (= 1 (count (:test5 err-resp))))

        (assert (str/includes? (:actual test5) "UnsupportedOperationException"))
        (assert (= {:context ""
                    :expected ""
                    :file "cljtest/error_test.clj"
                    :line 6
                    :type "fail" ; not "error"
                    :var "cljtest.error-test/test5"}
                   (select-keys test5 [:context :expected :file :line :type :var])))))))

(defn- run-midje-test
  []
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
                     :line 12
                     :type "fail"
                     :var "context4"}
                    {:actual "bar"
                     :context ""
                     :expected "baz"
                     :file "midjetest/fail_test.clj"
                     :line 13
                     :type "fail"
                     :var "context4"}]
                   :test3
                   [{:actual "true"
                     :context ""
                     :expected "false"
                     :file "midjetest/fail_test.clj"
                     :line 9
                     :type "fail"
                     :var "test3"}]}}
                 (:results resp))))))

(defn run-retest-test
  []
  (h/with-test-server [session]
    (doall (h/send-message session {:op "kaocha-test" :testable-ids ["unit"] :config-file "integration/clojure_test.edn"}))

    (assert (= "integration/clojure_test.edn"
               (get-in @kaocha/last-context [:config :config-file])))
    (assert (= #{:cljtest.fail-test/test3
                 :cljtest.fail-test/test4
                 :cljtest.error-test/test5}
               (set (:failed-testable-ids @kaocha/last-context))))

    (let [id (h/random-id)
          resp (-> session
                   (h/send-message {:id id :op "kaocha-retest"})
                   ensure-map)]
      (assert (= ["done"] (:status resp)))
      (assert (= id (:id resp)))
      (assert (= {:ns 2 :var 3 :test 4 :pass 0 :fail 3 :error 1}
                 (:summary resp)))
      (assert (= ["cljtest.error-test" "cljtest.fail-test"]
                 (-> (:testing-ns resp) (str/split  #"\s*,\s*")
                     sort)))
      (assert (= {:test3 [{:actual "false"
                           :context ""
                           :expected "false"
                           :file "cljtest/fail_test.clj"
                           :line 6
                           :type "fail"
                           :var "cljtest.fail-test/test3"}]
                  :test4 [{:actual "(not (= \"foo\" \"bar\"))"
                           :context "context4"
                           :expected "(= \"foo\" \"bar\")"
                           :file "cljtest/fail_test.clj"
                           :line 10
                           :type "fail"
                           :var "cljtest.fail-test/test4"}
                          {:actual "(not (= \"bar\" \"baz\"))"
                           :context "context4"
                           :expected "(= \"bar\" \"baz\")"
                           :file "cljtest/fail_test.clj"
                           :line 11
                           :type "fail"
                           :var "cljtest.fail-test/test4"}]}
                 (get-in resp [:results :cljtest.fail-test])))

      (let [err-resp (get-in resp [:results :cljtest.error-test])
            test5 (some-> err-resp :test5 first)]
        (assert (= [:test5] (-> err-resp keys sort)))
        (assert (= 1 (count (:test5 err-resp))))

        (assert (str/includes? (:actual test5) "UnsupportedOperationException"))
        (assert (= {:context ""
                    :expected ""
                    :file "cljtest/error_test.clj"
                    :line 6
                    :type "fail" ; not "error"
                    :var "cljtest.error-test/test5"}
                   (select-keys test5 [:context :expected :file :line :type :var])))))))

(defn -main
  []
  (println "\nIntegration tests: start >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
  (run-clojure-test-test)
  (run-midje-test)
  (run-retest-test)
  (println "Integration tests: end   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n")
  (System/exit 0))
