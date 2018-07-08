(ns proceed.lib.util
  (:require [clojure.string :as str]))

(defn log
  [& entries]
  (println (str/join " " entries)))

(defn log-comment
  [comment]
  (log (str "\n;; " comment)))

