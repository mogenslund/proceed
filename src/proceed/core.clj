(ns proceed.core
  (:require [clojure.string :as str]
            [proceed.db :as db]
            [hiccup.core :refer :all]
            [garden.core :refer [css]]
            [ring.middleware.params :refer :all]
            [org.httpkit.server :as server])
  (:gen-class))

;; (def aaa (-main))
;; (do (use 'proceed.db :reload) (use 'proceed.core :reload) (aaa) (def aaa (-main)))
;; (aaa) ; to stop

(def styling
  (css
    [:body {
       :background-color "#fefefe"
       :font-family "Arial, Helvetica, sans-serif"
       :font-size "small"}]
    [:h1 :h2 {
       :margin-top "30px"
       :margin-botton "0px"
       :color "#0085b6"}]
    [:table.main {
       :border-spacing 1
       :width "100%"
       :height "60%"
       :font-size "small"}]
    [:a {
       :text-decoration "none"}]
    [:td {
       :padding 8
       :width "50%"
       :height "50%"
       :text-align "left"
       :vertical-align "top"}]
    [:th {
       :background-color "#dddddd"
       :text-align "left"
       :vertical-align "top"}]
    [:tr {
       :background-color "#ffffff"}]
    [:span.task {
       :margin "6px"
       :padding "8px"
       :background-color "#aaaaaa"}]
     ))

(defn task-component
  [task]
  (html
    [:a {:href (str "/?task=" (task :id)) :auto (task :name)} [:span {:class "task"} (task :name)]]))

(defn task-matrix-component
  [tasks]
  (html
    [:table {:class "main"}
      [:tr [:th] [:th "Important"] [:th "Not Important"]]
      [:tr [:th "Urgent"]
           [:td {:bgcolor "#99cc00"} (map task-component (filter #(and (= (% :important) 1) (= (% :urgent) 1)) tasks))]
           [:td {:bgcolor "##36a4dd"} (map task-component (filter #(and (= (% :important) 1) (= (% :urgent) 0)) tasks))]]
      [:tr [:th "Not Urgent"]
           [:td {:bgcolor "#ff9f00"} (map task-component (filter #(and (= (% :important) 0) (= (% :urgent) 1)) tasks))]
           [:td {:bgcolor "#ff4d4e"} (map task-component (filter #(and (= (% :important) 0) (= (% :urgent) 0)) tasks))]]]
    ))

(defn task-details-component
  [task]
  (html
    [:form
      [:input {:type "text" :auto "taskname" :name (str "name" (task :id)) :value (task :name)}] 
      [:input {:type "text" :auto "taskdescription" :name (str "description" (task :id)) :value (task :description)}] 
      [:table {:style "display:inline" :auto "tasktable"}
        [:tr [:td {:bgcolor "#99cc00"}]
             [:td {:bgcolor "#cccccc"}]]
        [:tr [:td {:bgcolor "#cccccc"}]
             [:td {:bgcolor "#cccccc"}]]]]))

(defn main-page
  [tasks taskid]
  (html
    [:html
      [:header
        [:meta {:content "text/html;charset=utf-8"}]
        [:style styling]]
      [:body
        [:h1 "Task Manager"]
        (task-matrix-component tasks)
        (when taskid (task-details-component (first (filter #(= (% :id) taskid) tasks))))
      ]]))

(defn handler
  [request]
  (let [url (request :uri)
        params (request :query-params)
        fparams (request :form-params)
        method (request :request-method)
        taskid (when (params "task") (Integer/parseInt (params "task")))]
    {:status 200 :headers {"content-type" "text/html"} :body (main-page (db/read-tasks) taskid)}))
  

(defn -main
  [& args]
  (let [dbfile (if (empty? args) "/tmp/proceed.db" (first args))]
  (db/set-db-file dbfile)
  (when (not (.exists (clojure.java.io/file dbfile)))
    (db/reset-db))
  (server/run-server (wrap-params handler) {:port 3000})))
