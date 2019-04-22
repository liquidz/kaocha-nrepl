(ns kaocha-nrepl.kaocha-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :as t]
            [kaocha-nrepl.kaocha :as sut]))

(defn- read-testable [path]
   (-> (io/file path)
       slurp
       edn/read-string
      :kaocha.result/tests))

(def success-testable (read-testable "test/files/success_result.edn"))
(def fail-testable (read-testable "test/files/fail_result.edn"))

(def midje-fail-testable (read-testable "test/files/midje_fail_result.edn"))

(t/deftest eorror-test
  (t/testing "success"
    (t/is (= {} (sut/errors success-testable))))

  (t/testing "fail"
    (t/is (= {"kaocha-nrepl-dev.fail-test"
              {"test3" [{:type "fail"
                         :line 5
                         :context ""
                         :expected "false"
                         :actual "false"
                         :file "kaocha_nrepl_dev/fail_test.clj"
                         :var "kaocha-nrepl-dev.fail-test/test3"}]
               "test4" [{:type "fail"
                         :line 9
                         :context "context4"
                         :expected "foo"
                         :actual "(\"bar\")"
                         :file "kaocha_nrepl_dev/fail_test.clj"
                         :var "kaocha-nrepl-dev.fail-test/test4"}]}}
             (sut/errors fail-testable)))))

(t/deftest midje-error-test
  (t/testing "fail"
    (t/is (= {"midjetest.core-test"
              {"foobar" [{:type "fail"
                          :line 16
                          :context ""
                          :expected ""
                          :actual ""
                          :file "midjetest/core_test.clj"
                          :var "foobar"}]}}
             (sut/errors midje-fail-testable)))))

(t/deftest totals-test
  (t/testing "success"
    (t/is (= {:ns 1 :var 2 :test 3 :pass 3 :fail 0 :error 0}
             (sut/totals success-testable))))

  (t/testing "fail"
    (t/is (= {:ns 1 :var 2 :test 2 :pass 0 :fail 2 :error 0}
             (sut/totals fail-testable)))))

(t/deftest midje-totals-test
  (t/is (= {:ns 0 :var 1 :test 1 :pass 0 :fail 1 :error 0}
           (sut/totals midje-fail-testable))))

(t/deftest testing-ns-test
  (t/is (= "kaocha-nrepl-dev.success-test" (sut/testing-ns success-testable))))

(t/deftest midje-testing-ns-test
  (t/is (= "midjetest.core-test" (sut/testing-ns midje-fail-testable))))
