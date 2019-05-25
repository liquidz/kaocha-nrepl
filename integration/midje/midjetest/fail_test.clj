(ns midjetest.fail-test
  (:require [midje.config :as config]
            [midje.sweet :as midje]))

(config/change-defaults :print-level :print-nothing)

(midje/fact test3
  true => false)

(midje/facts "context4"
  (midje/fact test4
    "foo" => "bar"
    "bar" => "baz"))
