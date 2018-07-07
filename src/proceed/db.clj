(ns proceed.db
  (:require [clojure.java.jdbc :refer :all])
  (:gen-class))

(def db (atom 
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "/tmp/proceed.db"}))

(defn create-tables
  []
  (db-do-commands @db
    (create-table-ddl
      :tasks
      [[:id :integer "not null" "primary key" :autoincrement]
       [:name :text]
       [:description :text]
       [:category :integer]
       [:critical :integer]
       [:urgent :integer]]))
  (db-do-commands @db
    (create-table-ddl
      :categories
      [[:id :integer "not null" "primary key" :autoincrement]
       [:name :text]
       [:description :text]])))

(defn create-task
  [name description category critical urgent]
  (insert! @db :tasks
    {:name name
     :description description
     :category category
     :critical critical
     :urgent urgent}))

(defn read-task
  [id]
  (first (query @db (str "select * from tasks where id=" id))))

(defn read-tasks
  []
  (query @db "select * from tasks"))

(defn update-task
  [id name description category critical urgent]
  (update! @db :tasks
    {:name name
     :description description
     :category category
     :critical critical
     :urgent urgent}
    ["id = ?" id]))

(defn delete-task
  [id]
  (delete! @db :tasks
    ["id = ?" id]))

(defn create-category
  [name description]
  (insert! @db :categories
    {:name name
     :description description}))

(defn read-category
  [id]
  (first (query @db (str "select * from categories where id=" id))))

(defn read-categories
  []
  (query @db "select * from categories"))

(defn update-category
  [id name description]
  (update! @db :categories
    {:name name
     :description description}
    ["id = ?" id]))

(defn delete-category
  [id]
  (update! @db :tasks
    {:category nil} ["category = ?" id])
  (delete! @db :categories
    ["id = ?" id]))

(defn reset-db
  []
  (create-tables)
  (create-category "home" "")
  (create-category "work" "")
  (create-task "Clean car" "Clean up inside out" 1 0 1))

(defn set-db-file
  [filepath]
  (swap! db assoc :subname filepath))

; (clojure.java.io/delete-file "/tmp/proceed.db")
; (reset-db)
; (pr-str (read-tasks))
; (pr-str (read-categories))