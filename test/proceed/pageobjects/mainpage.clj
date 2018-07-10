(ns proceed.pageobjects.mainpage)

(def page-entry "http://localhost:3000")

(def main-button-task (partial format "//input[@auto='%s']"))
(def main-button-task-beer (main-button-task "Buy beer"))
(def main-button-task-beer (main-button-task "Take out fire"))