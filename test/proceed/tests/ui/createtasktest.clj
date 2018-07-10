(ns proceed.tests.ui.createtasktest
  (:require [clojure.test :refer :all]
            [proceed.pageobjects.mainpage :as main]
            [proceed.lib.web :refer :all]))


(deftest create-task-test
  (set-retries 20)
  (new-browser)
  (goto main/page-entry)
  (let [c (count (get-elements (main/main-button-task "New")))
        r (str (rand-int 10000))]
    (click "//input[@auto='new11']")
    (is (= (count (get-elements (main/main-button-task "New"))) (+ c 1)))
    (click (main/main-button-task "New"))
    (send-keys "//*[@auto='taskname']" r)
    (click "//*[@auto='savetask']")
    (is (> (count (get-elements (main/main-button-task (str "New" r)))) 0))
    (quit)))