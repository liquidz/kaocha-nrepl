(ns kaocha-nrepl.core
  (:require
   [kaocha-nrepl.kaocha :as kaocha]
   [nrepl.middleware :refer [set-descriptor!]]
   [nrepl.misc :refer [response-for]]
   [nrepl.transport :as transport]))

(def ^:private test-context (atom {}))

(defn- init-test-context!
  []
  (reset! test-context {}))

(defn- send!
  [m msg]
  (transport/send (:transport msg) (response-for msg m)))

(defn- progress-reporter
  [msg test _test-plan]
  (when-not (:kaocha.testable/skip test)
    (let [test-type (:kaocha.testable/type test)
          tests (some->> test
                         :kaocha.test-plan/tests
                         (remove :kaocha.testable/skip))]
      (when tests
        (swap! test-context assoc
               (-> tests first :kaocha.testable/type)
               {:total (count tests) :current 0}))

      (if-let [{:keys [current total]} (get @test-context test-type)]
        (do (swap! test-context update-in [test-type :current] inc)
            (send! {:out (format "Testing %s(%d/%d): %s"
                                 (name test-type)
                                 (inc current)
                                 total
                                 (str (:kaocha.testable/id test)))} msg))
        (send! {:out (format "Testing: %s" (str (:kaocha.testable/id test)))} msg))))
  test)

(defn- gen-config
  [msg]
  (let [{:keys [config-file disable-progress-reporter]
         :or {disable-progress-reporter false}} msg]
    (cond-> {}
      config-file
      (assoc :config-file config-file)

      (not disable-progress-reporter)
      (assoc :kaocha.hooks/pre-test [(partial progress-reporter msg)]))))

(defn- ensure-keyword-list
  [x]
  (some->> (cond-> x
             (not (sequential? x)) vector)
           (map keyword)))

(defn- test-all-reply
  [msg]
  (init-test-context!)
  (-> (gen-config msg)
      kaocha/run-all
      (merge {:status :done})
      (send! msg)))

(defn- test-reply
  [msg]
  (init-test-context!)
  (let [{:keys [testable-ids]} msg
        config (gen-config msg)
        run-args (some-> testable-ids ensure-keyword-list (concat [config]))]
    (if run-args
      (-> (apply kaocha/run run-args)
          (merge {:status :done})
          (send! msg))
      (send! {:error "Invalid testable ids"} msg))))

(defn- testable-ids-reply
  [msg]
  (-> (gen-config msg)
      kaocha/testable-ids
      (merge {:status :done})
      (send! msg)))

(defn- retest-reply
  [msg]
  (init-test-context!)
  (-> (kaocha/rerun)
      (merge {:status :done})
      (send! msg)))

(defn wrap-kaocha
  [handler]
  (fn [{:keys [op] :as msg}]
    (case op
      "kaocha-test-all" (test-all-reply msg)
      "kaocha-test" (test-reply msg)
      "kaocha-testable-ids" (testable-ids-reply msg)
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

              "kaocha-testable-ids"
              {:doc "Return testable ids"
               :requires {}
               :optional {"config-file" "Configuration file for kaocha"}}

              "kaocha-retest"
              {:doc "Rerun last failed tests"
               :requires {}
               :optional {}}}}))
