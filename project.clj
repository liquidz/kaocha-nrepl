(defproject kaocha-nrepl "0.1.0-SNAPSHOT"
  :description "nREPL Middleware Example"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [nrepl "0.6.0"]
                 [lambdaisland/kaocha "0.0-418"]]
  :profiles
  {:dev {:source-paths ["dev" "src"]}})
