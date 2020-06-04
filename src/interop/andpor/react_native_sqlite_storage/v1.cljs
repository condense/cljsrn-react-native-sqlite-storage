(ns interop.andpor.react-native-sqlite-storage.v1
  (:require [react-native-sqlite-storage :as module]
            [goog.object :as gobject]))

(assert module)

; https://github.com/andpor/react-native-sqlite-storage/blob/master/lib/sqlite.core.js

; Types
(defn js-constructor-name [x] (some-> x .-constructor .-name))
(defn SQLiteFactory? [x] (= "SQLiteFactory" (js-constructor-name x)))
(defn SQLitePluginTransaction? [x] (= "SQLitePluginTransaction" (js-constructor-name x)))
(defn SQLitePlugin? [x] (= "SQLitePlugin" (js-constructor-name x)))

; Helpers
(defn data [o] (zipmap (map keyword (gobject/getKeys o)) (gobject/getValues o)))
(defn rows-data [{:keys [rows] :as m}] (cond-> m rows (assoc :rows (mapv #(data (.item rows %)) (range (gobject/get rows "length"))))))

; API
(defn open-database [openargs success error] (module/openDatabase (clj->js openargs) success error))
(defn DEBUG [debug] (module/DEBUG debug))
(defn sqlite-features [] (js->clj (module/sqliteFeatures)))
(defn echo-test [success error] (module/echoTest success error))
(defn delete-database [openargs success error] (module/deleteDatabase (clj->js openargs) success error))
(defn sql-batch [o sqlStatements success error] (.sqlBatch o (clj->js sqlStatements) success error))
(defn open [o success error] (.open o success error))
(defn close [o success error] (.close o success error))
(defn execute-sql [o sql values success error] (.executeSql o sql (into-array values) (comp success rows-data data) (comp error data)))
(defn attach [o dbNameToAttach dbAlias success error] (.attach o dbNameToAttach dbAlias success error))
(defn detach [o dbAlias success error] (.detach o dbAlias success error))
(defn add-transaction [o t] (.addTransaction o t))
(defn transaction [o fn error success] (.transaction o fn error success))
(defn read-transaction [o fn error success] (.readTransaction o fn error success))
(defn start-next-transaction [o] (.startNextTransaction o))
(defn abort-all-pending-transactions [o] (.abortAllPendingTransactions o))
(defn start [o] (.start o))
(defn handle-statement-success [o handler response] (.handleStatementSuccess o handler response))
(defn handle-statement-failure [o handler response] (.handleStatementFailure o handler response))
(defn run [o] (.run o))
(defn abort [o txFailure] (.abort o txFailure))
(defn finish [o] (.finish o))
(defn abort-from-q [o sqlerror] (.abortFromQ o sqlerror))
(defn add-statement [o sql values success error]
  (.addStatement o sql (into-array values)
                 (fn [tx results] (success tx (rows-data (data results))))
                 (fn [tx err] (error tx (data err)))))

(comment

  (do (delete-database {:name "user.db"} log err)
      (delete-database {:name "addr.db"} log err))

  (do (def log (partial println :log))
      (def err (partial println :err))
      (def db1 (open-database {:name "user.db"}))
      (def db2 (open-database {:name "addr.db"})))

  (sql-batch db1 ["CREATE TABLE IF NOT EXISTS  USER (NAME, EMAIL)"
                  ["INSERT INTO USER (NAME, EMAIL) VALUES (?,?)" ["Foo" "foo@example.com"]]
                  ["INSERT INTO USER (NAME, EMAIL) VALUES (?,?)" ["Bar" "bar@example.com"]]] log err)

  (sql-batch db2 ["CREATE TABLE IF NOT EXISTS  USER_ADDRESS (NAME, ADDRESS)"
                  ["INSERT INTO USER_ADDRESS (NAME, ADDRESS) VALUES (?,?)" ["Bar" "Bar Avenue"]]
                  ["INSERT INTO USER_ADDRESS (NAME, ADDRESS) VALUES (?,?)" ["Baz" "Baz Street"]]] log err)

  (execute-sql db1 "SELECT * FROM USER" [] log err)

  (open db1 log err)
  (close db1 log err)

  (attach db1 "addr.db" "ALIAS2" log err)
  (execute-sql db1 "SELECT USER.EMAIL, USER_ADDRESS.ADDRESS FROM USER NATURAL JOIN USER_ADDRESS" [] log err)
  (detach db1 db2 "ALIAS2" log err)

  (execute-sql db1 "DROP TABLE IF EXISTS TX_TEST" [] log err)
  (transaction db1 (fn [tx]
                     (add-statement tx "CREATE TABLE IF NOT EXISTS TX_TEST (COL1)" [] log err)
                     (add-statement tx "DELETE FROM TX_TEST" [] log err)
                     (add-statement tx "INSERT INTO TX_TEST (COL1) VALUES (?)" [1] log err)
                     (add-statement tx "INSERT INTO TX_TEST1 (COL1) VALUES (?)" [2] log err)) log err)
  (execute-sql db1 "SELECT * FROM TX_TEST" [] log err))
