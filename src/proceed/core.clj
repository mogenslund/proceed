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
       :font-size "12px"}]
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
    ["input[type='text']" {
       :font-size "12px"}]
    ["input[type='submit']" {
       :border "none"
       :color "white"
       :padding "2px 10px 2px 10px"
       :text-align "center"
       :text-decoration "none"
       :display "inline-block"
       :font-size "12px"}]
    [:.greenbutton {
       :background-color "#4CAF50"}]
    [:.redbutton {
       :background-color "#AA0000"}]
    [:.taskbutton {
       :background-color "#aaaaaa"}]
    [:.movebutton {
       :background-color "#008CBA"}]
    ))

(defn task-button
  [task]
  (html
    [:form {:action "/" :style "display:inline;"}
      [:input {:type "hidden" :name "task" :value (task :id)}]
      " " [:input {:type "submit" :taskid (task :id) :class "taskbutton" :auto (task :name) :value (task :name) :draggable "true" :ondragstart "drag(event)"}]]))

(defn new-button
  [important urgent]
  (html
    [:form {:action "/" :method :post :style "display:inline;"}
      [:input {:type "hidden" :name "important" :value important}]
      [:input {:type "hidden" :name "urgent" :value urgent}]
      " " [:input {:type "submit" :class "greenbutton" :auto (str "new" important urgent) :value "New"}]]))

(defn move-button
  [important urgent]
  (html
    [:form {:method :post :style "display:inline;" :id (str "move" important urgent)}
      [:input {:type "hidden" :name "moveimportant" :value important}]
      [:input {:type "hidden" :name "moveurgent" :value urgent}]
      " " [:input {:type "submit" :class "movebutton" :auto (str "move" important urgent) :value "Move"}]]))

(defn task-matrix-cell
  [tasks important urgent color]
  (html
    [:td {:bgcolor color :ondrop "drop(event)" :ondragover "allowDrop(event)" :id (str "e" important urgent)}
      (map task-button (filter #(and (= (% :important) important) (= (% :urgent) urgent)) tasks))
      [:br][:br][:br]
      (new-button important urgent)
      (move-button important urgent)]))


(defn task-matrix-component
  [tasks]
  (html
    [:table {:class "main"}
      [:tr [:th] [:th "Important"] [:th "Not Important"]]
      [:tr [:th "Urgent"]
             (task-matrix-cell tasks 1 1 "#99cc00")
             (task-matrix-cell tasks 1 0 "#36a4dd")]
      [:tr [:th "Not Urgent"]
             (task-matrix-cell tasks 0 1 "#ff9f00")
             (task-matrix-cell tasks 0 0 "#ff4d4e")]]))

(defn task-details-component
  [task]
  (when task
    (html
      [:br]
      [:form {:method "post"  :style "display:inline;"}
        [:input {:type "text" :auto "taskname" :name "taskname" :value (task :name)}] 
        [:input {:type "text" :auto "taskdescription" :name "taskdescription" :value (task :description)}] 
        " " [:input {:type "submit" :auto "savetask" :class "greenbutton" :value "Save"}]]
      [:form {:method "post" :style "display:inline;"}
        [:input {:type "hidden" :name "taskdelete" :value "delete"}]
        " " [:input {:type "submit" :class "redbutton" :value "Delete"}]])))

(defn main-page
  [tasks taskid]
  (html
    [:html
      [:header
        [:meta {:content "text/html;charset=utf-8"}]
        [:style styling]
        [:script (str
                   "function allowDrop(ev) {ev.preventDefault();}\n"
                   "function drag(ev) {\n"
                   "console.log(ev);\n"
                   "  ev.dataTransfer.setData('taskid', ev.srcElement.attributes.taskid.value);\n"
                   "}\n"
                   "function drop(ev) {\n"
                   "ev.preventDefault();\n"
                   "var data = ev.dataTransfer.getData('taskid');\n"
                   "    document.getElementById('mov' + ev.target.id).action = '/?task=' + data;"
                   "    document.getElementById('mov' + ev.target.id).submit();"
                   "}")]]
      [:body
        [:h1 "Task Manager"]
        (task-matrix-component tasks)
        (when taskid (task-details-component (first (filter #(= (% :id) taskid) tasks))))
      ]]))

(def tmp-req (atom ""))
; (println @tmp-req)

(defn handler
  [request]
  (let [url (request :uri)
        params (request :query-params)
        fparams (request :form-params)
        method (request :request-method)
        taskid (when (params "task") (Integer/parseInt (params "task")))]
    (reset! tmp-req (pr-str request))
    (when (= method :post)
      (cond (contains? fparams "taskdelete")
              (db/delete-task taskid)
            (contains? fparams "taskname")
              (db/update-task taskid :name (fparams "taskname") :description (fparams "taskdescription"))
            (contains? fparams "important")
              (db/create-task :name "New" :important (fparams "important") :urgent (fparams "urgent"))
            (contains? fparams "moveimportant")
              (db/update-task taskid :important (fparams "moveimportant") :urgent (fparams "moveurgent"))))
    {:status 200
     :headers {"content-type" "text/html"}
     :body (main-page (db/read-tasks) taskid)}))
  

(defn -main
  [& args]
  (let [dbfile (if (empty? args) "/tmp/proceed.db" (first args))]
    (db/set-db-file dbfile)
    (when (not (.exists (clojure.java.io/file dbfile)))
      (db/reset-db))
    (server/run-server (wrap-params handler) {:port 3000})))
