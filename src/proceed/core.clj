(ns proceed.core
  (:require [clojure.string :as str]
            [proceed.db :as db]
            [hiccup.core :refer :all]
            [ring.middleware.params :refer :all]
            [org.httpkit.server :as server])
  (:gen-class))

;; (def aaa (-main))
;; (do (use 'proceed.db :reload) (use 'proceed.core :reload) (aaa) (def aaa (-main)))
;; (aaa) ; to stop

(def css
  (str
    "\nbody {\n"
    "  background-color: #fefefe;\n"
    "  font-family: Arial, Helvetica, sans-serif;\n"
    "  font-size: small;\n"
    "}\n"
    "h1, h2 {\n"
    "  margin-top: 30px;\n"
    "  margin-botton: 0px;\n"
    "  color: #0085b6;\n"
    "}\n"
    "table.main {\n"
    "  border-spacing: 1;\n"
    "  width: 100%;\n"
    "  height: 60%;\n"
    "  font-size: small;\n"
    "}\n"
    "a {\n"
    "  text-decoration: none;\n"
    "}\n"
    "td {\n"
    "  padding: 8;\n"
    "  width: 50%;\n"
    "  height: 50%;\n"
    "  text-align: left;\n"
    "  vertical-align: top;\n"
    "}\n"
    "th {\n"
    "  background-color: #dddddd;\n"
    "  text-align: left;\n"
    "  vertical-align: top;\n"
    "}\n"
    "tr {\n"
    "  background-color: #ffffff;\n"
    "}\n"
    "span.task {\n"
    "  margin: 6px;\n"
    "  padding: 8px;\n"
    "  background-color: #aaaaaa;\n"
    "}\n"
   ))

(defn task-component
  [task]
  (html
    [:a {:href (str "/?task=" (task :id))} [:span {:class "task"} (task :name)]]))

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
      [:input {:type "text" :name (str "name" (task :id)) :value (task :name)}] 
      [:input {:type "text" :name (str "description" (task :id)) :value (task :description)}] 
      [:table {:style "display:inline"}
        [:tr [:td {:bgcolor "#99cc00"}]
             [:td {:bgcolor "#cccccc"}]]
        [:tr [:td {:bgcolor "#cccccc"}]
             [:td {:bgcolor "#cccccc"}]]]]))

(defn main-page
  [tasks taskid]
  (println (pr-str taskid))
  (html
    [:html
      [:header
        [:meta {:content "text/html;charset=utf-8"}]
        [:style css]]
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
    (println "!!!!" (pr-str request))
    (println "!!!!" params)
    (println "!!!!" taskid)
    {:status 200 :headers {"content-type" "text/html"} :body (main-page (db/read-tasks) taskid)}))
  

(defn -main
  [& args]
  (let [dbfile (if (empty? args) "/tmp/proceed.db" (first args))]
  (db/set-db-file dbfile)
  (when (not (.exists (clojure.java.io/file dbfile)))
    (db/reset-db))
  (server/run-server (wrap-params handler) {:port 3000})))
