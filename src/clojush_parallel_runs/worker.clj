(ns clojush-parallel-runs.worker
  (:use [clojush-parallel-runs.rabbitmq :as mq]
        [clojure.contrib.duck-streams :only [with-out-writer file-str]]))

(defn process
  [params file output-dir]
  (let [output-file (file-str (str output-dir "/" (:name params) "_" (System/currentTimeMillis)))]
    (with-out-writer output-file
      (let [run-pointer (load-file file)
            succ-gen (run-pointer params)]
        (mq/send-one "in" {:name (:name params)
                           :paramaters-used params
                           :log-file (.getName output-file)
                           :success-generation succ-gen})))))

(defn start-worker
  "Process params from the queue (if none, waits 10 seconds)."
  [connection-creds queue-name file output-dir]
  (init-connection connection-creds)
  (loop [msg (mq/get-one queue-name)]
    (if (not (nil? msg))
      (process msg file output-dir)
      (Thread/sleep 10000))
    (recur (mq/get-one queue-name))))
