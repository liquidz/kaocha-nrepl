(ns kaocha-nrepl.core-test
  (:require [clojure.test :as t]
            [kaocha-nrepl.core :as sut]
            [nrepl.core :as nrepl]
            [nrepl.server :as server]))

(def ^:dynamic *server* nil)

(defn repl-server-fixture
  [f]
  (with-open [server (server/start-server :handler (server/default-handler #'sut/wrap-kaocha))]
    (binding [*server* server]
      (f))))

(t/use-fixtures :each repl-server-fixture)

(defn- ensure-map [x]
  (cond
    (sequential? x) (apply merge x)
    :else x))

(t/deftest dummy-test
  (t/testing "foo"
    (t/is true)
    ;;(t/is (= 1 "testdata"))
    ))

(t/deftest kaocha-test-ns-test
  (with-open [transport (nrepl/connect :port (:port *server*))]
    (let [client (nrepl/client transport Long/MAX_VALUE)
          id (str (java.util.UUID/randomUUID))
          responses (nrepl/message client {:op "kaocha-test-ns" :id id
                                                      :ns "kaocha-nrepl.core-test/dummy-test"
                                                      })
          response (ensure-map responses)]
      (t/is (= (:status response) ["done"]))
      ;;(t/is (nil? responses))
      (t/is (= id (:id response))))))
