(ns cljtest.fail-test
  (:require
   [clojure.test :as t]))

(t/deftest test3
  (t/is false))

(t/deftest test4
  (t/testing "context4"
    (t/is (= "foo" "bar"))
    (t/is (= "bar" "baz"))))
