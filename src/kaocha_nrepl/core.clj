(ns kaocha-nrepl.core
  (:require [kaocha-nrepl.kaocha :as kaocha]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]))

(defn- send! [m msg]
  (transport/send (:transport msg) (response-for msg m)))

(defn- test-all-reply [msg]
  (let [{:keys [config-file]} msg
        config (cond-> {}
                 config-file (assoc :config-file config-file))]
    (-> (kaocha/run-all config)
        (merge {:status :done})
        (send! msg))))

(defn- ensure-list [x]
  (cond-> x
    (not (sequential? x)) vector))

(defn- test-reply [msg]
  (let [{:keys [config-file testable-ids]} msg
        config (cond-> {}
                 config-file (assoc :config-file config-file))
        run-args (some-> testable-ids ensure-list (concat [config]))]
    (if run-args
      (-> (apply kaocha/run run-args)
          (merge {:status :done})
          (send! msg))
      (send! {:error "Invalid testable ids"} msg))))

(defn- retest-reply [msg]
  (-> (kaocha/rerun)
      (merge {:status :done})
      (send! msg)))

(defn wrap-kaocha [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      "kaocha-test-all" (test-all-reply msg)
      "kaocha-test" (test-reply msg)
      "kaocha-retest" (retest-reply msg)
      (handler msg))))

(when (resolve 'set-descriptor!)
  (set-descriptor!
   #'wrap-kaocha
   {:doc "nREPL wrapper for kaocha"
    :requires #{}
    :handles {"kaocha-test-all"
              {:doc "Run all test"
               :requires {}
               :optional {"config-file" "Configuration file for kaocha"}}

              "kaocha-test"
              {:doc "Run tests by testable ids"
               :requires {"testable-ids" "List of testable id"}
               :optional {"config-file" "Configuration file for kaocha"}}

              "kaocha-retest"
              {:doc "Rerun last failed tests"
               :requires {}
               :optional {}}}}))
