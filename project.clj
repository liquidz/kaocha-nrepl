(defproject kaocha-nrepl "1.0.5-SNAPSHOT"
  :description "nREPL Middleware for kaocha"
  :url "https://github.com/liquidz/kaocha-nrepl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[nrepl "0.8.3"]
                 [lambdaisland/kaocha "1.0.732"
                  :exclusions [org.clojure/clojure]]]

  :plugins [[lein-cloverage "1.2.2"]]

  :profiles
  {:dev {:source-paths ["dev" "src"]
         :dependencies [[lambdaisland/kaocha-midje "0.0-5"
                         :exclusions [org.clojure/clojure
                                      lambdaisland/kaocha
                                      midje/midje]]
                        [midje/midje "1.9.9"
                         :exclusions [org.clojure/clojure]]]}

   :1.9 [:dev {:dependencies [[org.clojure/clojure "1.10.2"]]}]
   :1.10 [:dev {:dependencies [[org.clojure/clojure "1.10.2"]]}]
   :1.10.2 [:dev {:dependencies [[org.clojure/clojure "1.10.2"]]}]
   :release {:dependencies [[org.clojure/clojure "1.10.2"]]}
   :antq {:dependencies [[antq "RELEASE"]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :aliases
  {"integration-test" ["with-profile" "1.9:1.10:1.10.2"
                       "run" "-m" "integration-test"]
   "test-all" ["do" ["with-profile" "1.9:1.10:1.10.2" "test"] ["integration-test"]]})
