(defproject proceed "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [ring/ring-core "1.7.0-RC1"]
                 [http-kit "2.3.0"]
                 [hiccup "2.0.0-alpha1"]
                 [garden "1.3.5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.clojure/java.jdbc "0.7.6"]]
  :main ^:skip-aot proceed.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :edit {:dependencies [[mogenslund/liquid "0.9.3"]]
                    :main dk.salza.liq.core}
             :test {:dependencies [[org.seleniumhq.selenium/selenium-chrome-driver "3.9.1"]
                                   [org.seleniumhq.selenium/selenium-support "3.9.1"]]}}
  :aliases {"edit" ["with-profile" "edit" "run"]})
