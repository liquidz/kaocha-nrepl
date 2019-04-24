(ns midjetest.fail-test
  (:require [midje.sweet :as midje]))

(midje/fact test3
  true => false)

(midje/facts "context4"
  (midje/fact test4
    "foo" => "bar"
    "bar" => "baz"))
