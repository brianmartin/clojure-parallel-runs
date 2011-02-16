(ns clojure-parallel-runs.worker
  (:use [clojure-parallel-runs.rabbitmq :as mq]
        [clojure.contrib.duck-streams :only [with-out-writer file-str]]))

(defn log-file-path 
  "Given 'logs' and 'name', outputs a File for 'logs/name/Wed_Feb_16_12:50:36_EST_20111297878636191"
  [output-dir param-name]
  (if output-dir
    (file-str (str output-dir "/" param-name "/"
                (-> (java.util.Date.) (.toString) (.replace \space \_) (.concat (str (System/currentTimeMillis))))))))

(defn process
  [params q-in output-dir]
  (let [output-file (log-file-path output-dir (:name params))
        run (fn []
              (let [run-pointer (load-file (params :file))
                    return-value (run-pointer params)]
                (mq/send-one q-in {:name (:name params)
                                   :paramaters-used params
                                   :log-file (.getName output-file)
                                   :return-value return-value})))]
    (if output-file (with-out-writer output-file (run)) (run))))

(defn start-worker
  "Process params from the queue (if no more messages, quit)."
  [connection-creds q-out q-in output-dir]
  (init-connection connection-creds)
  (loop [msg (mq/get-one q-out)]
    (if (not (nil? msg))
      (do
        (process msg q-in output-dir)
        (recur (mq/get-one q-out))))))
