(ns cljtest.success-test
  (:require
   [clojure.test :as t]))

(t/deftest test1
  (t/is true))

(t/deftest test2
  (t/testing "context2"
    (t/is (= 1 1))
    (t/is (= 2 2))))
