(ns proceed.core
  (:require [clojure.string :as str]
            [proceed.db :as db]
            [hiccup.core :refer :all]
            [ring.middleware.params :refer :all]
            [org.httpkit.server :as server])
  (:gen-class))

(defn -main
  [& args]
  (let [dbfile (if (empty? args) "/tmp/proceed.db" (first args))]
  (db/set-db-file dbfile)
  (when (not (.exists (clojure.java.io/file dbfile)))
    (db/reset-db))
  (server/run-server (wrap-params handler) {:port 3000})))
