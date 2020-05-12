(ns cljtest.error-test
  (:require
   [clojure.test :as t]))

(t/deftest test5
  (let [[foo] {:foo 1}]
    (t/is (= 1 foo))))
