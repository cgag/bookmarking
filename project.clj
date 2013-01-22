(defproject bookmarking "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.0-RC2"]
                 [compojure "1.1.1"]
                 [cheshire "5.0.1"]
                 [com.cemerick/url "0.0.7"]
                 [korma "0.3.0-beta13"]
                 [postgresql "9.1-901.jdbc4"]
                 [org.clojars.leonardoborges/lobos "1.0.3-SNAPSHOT"]
                 [hiccup "1.0.1"]
                 [com.cemerick/friend "0.1.2"]
                 [com.novemberain/validateur "1.2.0"]
                 [environ "0.3.0"]
                 [sandbar/sandbar-auth "0.2.4"]]
  :dev-dependencies [[ring-serve "0.1.1"]]
  :plugins [[lein-ring "0.7.3"]
            [lein-cljsbuild "0.2.10"]]
  ;; TODO: try using ring-mock
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})