(ns clojush-parallel-runs.worker
  (:use [clojush-parallel-runs.rabbitmq :as mq]
        [clojure.contrib.duck-streams :only [with-out-writer file-str]]))

(defn process
  [params file output-dir q-in]
  (let [output-file (file-str (str output-dir "/" (:name params) "_" (System/currentTimeMillis)))]
    (with-out-writer output-file
      (let [run-pointer (load-file file)
            succ-gen (run-pointer params)]
        (mq/send-one q-in {:name (:name params)
                           :paramaters-used params
                           :log-file (.getName output-file)
                           :success-generation succ-gen})))))

(defn start-worker
  "Process params from the queue (if none, waits 10 seconds)."
  [connection-creds q-out q-in file output-dir]
  (init-connection connection-creds)
  (loop [msg (mq/get-one q-out)]
    (if (not (nil? msg))
      (do
        (process msg file output-dir q-in)
        (recur (mq/get-one q-out))))))
