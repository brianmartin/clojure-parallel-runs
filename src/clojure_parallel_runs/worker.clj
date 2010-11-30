(ns clojure-parallel-runs.worker
  (:use [clojure-parallel-runs.rabbitmq :as mq]
        [clojure.contrib.duck-streams :only [with-out-writer file-str]]))

(defn process-without-log
  [params file q-in]
  (let [run-pointer (load-file file)
        return-value (run-pointer params)]
    (mq/send-one q-in {:name (:name params)
                       :paramaters-used params
                       :log-file nil
                       :return-value return-value})))

(defn process-with-log
  [params file output-dir q-in]
  (let [output-file (file-str (str output-dir "/" (:name params) "_" (System/currentTimeMillis)))]
    (with-out-writer output-file
      (let [run-pointer (load-file file)
            return-value (run-pointer params)]
        (mq/send-one q-in {:name (:name params)
                           :paramaters-used params
                           :log-file (.getName output-file)
                           :return-value return-value})))))

(defn start-worker
  "Process params from the queue (if no more messages, quit)."
  [connection-creds q-out q-in file output-dir]
  (init-connection connection-creds)
  (loop [msg (mq/get-one q-out)]
    (if (not (nil? msg))
      (do
        (if output-dir
          (process-with-log msg file output-dir q-in)
          (process-without-log msg file q-in))
        (recur (mq/get-one q-out))))))
