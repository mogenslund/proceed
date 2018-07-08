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
    [:.newbutton {
       :background-color "#4CAF50"
       :border "none"
       :color "white"
       :padding "2px 2px"
       :text-align "center"
       :text-decoration "none"
       :display "inline-block"
       :font-size "12px"}]
    [:.movebutton {
       :background-color "#008CBA"
       :border "none"
       :color "white"
       :padding "2px 2px"
       :text-align "center"
       :text-decoration "none"
       :display "inline-block"
       :font-size "12px"}]
     ))

(defn task-component
  [task]
  (html
    [:a {:href (str "/?task=" (task :id)) :auto (task :name)} [:span {:class "task"} (str (task :name) " ")]]))

(defn new-button
  [important urgent]
  (html
    [:form {:action "/" :method :post :style "display:inline;"}
      [:input {:type "hidden" :name "important" :value important}]
      [:input {:type "hidden" :name "urgent" :value urgent}]
      [:input {:type "submit" :class "newbutton" :auto (str "new" important urgent) :value "New"}]]))

(defn move-button
  [important urgent]
  (html
    [:form {:method :post :style "display:inline;"}
      [:input {:type "hidden" :name "moveimportant" :value important}]
      [:input {:type "hidden" :name "moveurgent" :value urgent}]
      [:input {:type "submit" :class "movebutton" :auto (str "move" important urgent) :value "Move"}]]))

(defn task-matrix-cell
  [tasks important urgent color]
  (html
    [:td {:bgcolor color}
      (map task-component (filter #(and (= (% :important) important) (= (% :urgent) urgent)) tasks))
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
  (html
    [:br]
    [:form {:method "post"}
      [:input {:type "text" :auto "taskname" :name "taskname" :value (task :name)}] 
      [:input {:type "text" :auto "taskdescription" :name "taskdescription" :value (task :description)}] 
      [:input {:type "submit" :value "Save"}]]))

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
      (when (contains? fparams "taskname")
        (db/update-task taskid {:name (fparams "taskname") :description (fparams "taskdescription")}))
      (when (contains? fparams "important")
        (db/create-task "New" "" 1 (fparams "important") (fparams "urgent")))
      (when (contains? fparams "moveimportant")
        (db/update-task taskid {:important (fparams "moveimportant") :urgent (fparams "moveurgent")}))
    )
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
