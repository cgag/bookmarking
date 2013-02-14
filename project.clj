(defproject bookmarking "0.1.0-SNAPSHOT"
  :main bookmarking.handler
  :description "A simple bookmarking service inspired by Instapaper."
  :url "bookmarking.curtis.io"
  :dependencies [[org.clojure/clojure "1.5.0-RC16"]
                 [compojure "1.1.5" :exclusions [org.clojure/core.incubator
                                                 org.clojure/tools.macro
                                                 ring/ring-core]]
                 [cheshire "5.0.1"]
                 [enlive "1.0.1"]
                 [com.cemerick/url "0.0.7" :exclusions [org.clojure/core.incubator]]
                 [korma "0.3.0-beta13"]
                 [postgresql "9.1-901.jdbc4"]
                 [lobos "1.0.0-beta1"]
                 [hiccup "1.0.2"]
                 [com.cemerick/friend "0.1.3" :exclusions [net.sourceforge.nekohtml/nekohtml
                                                           org.apache.httpcomponents/httpclient
                                                           slingshot
                                                           ring/ring-core]]
                 [com.novemberain/validateur "1.2.0"]
                 [environ "0.3.0"]
                 [clj-http "0.6.4"]
                 [io.curtis/boilerpipe-clj "0.1.2"]
                 [ring-server "0.2.7"]]
  :dev-dependencies []
  :plugins [[lein-ring "0.7.3"]
            [lein-cljsbuild "0.2.10"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})
