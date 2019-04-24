(ns midjetest.success-test
  (:require [midje.sweet :as midje]))

(midje/fact test1
  true => true)

(midje/facts "context2"
  (midje/fact test2
    1 => 1
    2 => 2))

