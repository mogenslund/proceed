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
       [:important :integer]
       [:urgent :integer]
       [:done :integer]]))
  (db-do-commands @db
    (create-table-ddl
      :categories
      [[:id :integer "not null" "primary key" :autoincrement]
       [:name :text]
       [:description :text]])))

(defn create-task
  [& data]
  (let [defaults
         {:name "New"
          :description ""
          :category 1
          :important 1
          :urgent 1
          :done 0}]
    (first (vals (first (insert! @db :tasks (merge defaults (apply hash-map data))))))))
          
(defn read-task
  [id]
  (first (query @db (str "select * from tasks where id=" id))))

(defn read-tasks
  []
  (query @db "select * from tasks"))
  

(defn update-task
  [id & data]
  (update! @db :tasks
    (apply hash-map data)
    ["id = ?" id]))
   

(defn delete-task
  [id]
  (delete! @db :tasks
    ["id = ?" id]))

(defn create-category
  [& data]
  (let [defaults
         {:name "New"
          :description ""}]
    (insert! @db :categories (merge defaults (apply hash-map data)))))

(defn read-category
  [id]
  (first (query @db (str "select * from categories where id=" id))))

(defn read-categories
  []
  (query @db "select * from categories"))

(defn update-category
  [id & data]
  (update! @db :categories
    (apply hash-map data)
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
  (create-category :name "home")
  (create-category :name "work")
  (create-task :name "Buy beer" :urgent 0)
  (create-task :name "Make everyone happy")
  (create-task :name "Take out fire")
  (create-task :name "Watch soccer")
  (create-task :name "Clean car" :description "Clean up inside out"))

(defn set-db-file
  [filepath]
  (swap! db assoc :subname filepath))

; (clojure.java.io/delete-file "/tmp/proceed.db")
; (reset-db)
; (pr-str (read-tasks))
; (pr-str (read-categories))
; (pr-str (create-task :name "Make everyone happy"))