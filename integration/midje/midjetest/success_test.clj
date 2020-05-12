(ns midjetest.success-test
  (:require
   [midje.config :as config]
   [midje.sweet :as midje]))

(config/change-defaults :print-level :print-nothing)

(midje/fact test1
  true => true)

(midje/facts "context2"
  (midje/fact test2
    1 => 1
    2 => 2))
