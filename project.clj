(defproject kaocha-nrepl "0.1.0-SNAPSHOT"
  :description "nREPL Middleware for kaocha"
  :url "https://github.com/liquidz/kaocha-nrepl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [nrepl "0.6.0"]
                 [lambdaisland/kaocha "0.0-418"]]

  :profiles
  {:dev {:source-paths ["dev" "src"]
         :dependencies [[lambdaisland/kaocha-midje "0.0-5" :exclusions [midje/midje]]
                        [midje/midje "1.9.6"]]}}

  :aliases
  {"integration-test" ["with-profile" "+dev"
                       "run" "-m" "integration-test"]
   "test-all" ["do" ["test"] ["integration-test"]]})
