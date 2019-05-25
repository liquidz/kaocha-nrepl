(defproject kaocha-nrepl "0.1.0"
  :description "nREPL Middleware for kaocha"
  :url "https://github.com/liquidz/kaocha-nrepl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[nrepl "0.6.0"]
                 [lambdaisland/kaocha "0.0-418"
                  :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev {:source-paths ["dev" "src"]
         :dependencies [[lambdaisland/kaocha-midje "0.0-5"
                         :exclusions [org.clojure/clojure
                                      lambdaisland/kaocha
                                      midje/midje]]
                        [midje/midje "1.9.8"
                         :exclusions [org.clojure/clojure]]]}

   :1.9 [:dev {:dependencies [[org.clojure/clojure "1.9.0"]]}]
   :1.10 [:dev {:dependencies [[org.clojure/clojure "1.10.0"]]}]}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :aliases
  {"integration-test" ["with-profile" "1.9:1.10"
                       "run" "-m" "integration-test"]
   "test-all" ["do" ["with-profile" "1.9:1.10" "test"] ["integration-test"]]})
