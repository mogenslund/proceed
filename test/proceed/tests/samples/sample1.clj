(ns proceed.tests.samples.sample1
  (:require [clojure.test :refer :all]
            [proceed.pageobjects.mainpage :as main]
            [proceed.lib.web :refer :all]))

(deftest add
  (is (= 1 (+ 0 1)) "Not working"))

(deftest not-so-good
  (is (= 1 (+ 1 0)) "Not working"))

(deftest searchtest1
  (set-retries 20)
  (new-browser)
  (goto main/page-entry)
  (click main/main-button-task-fire)
  (click main/main-button-task-beer)
  (click main/main-button-task-fire)
  (quit))

