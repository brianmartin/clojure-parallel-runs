(ns clojush-parallel-runs.core
  (:use [clojush-parallel-runs.rabbitmq :as mq]
        [clojush-parallel-runs.worker :as w]
        [clojure.contrib.duck-streams :only [with-out-append-writer]]
        [clojure.contrib.command-line])
  (:gen-class))

(defn distribute 
  "Distributes a sequence of objects 
  (map, vector, seq, java obj, whatever)."
  [connection-params msgs]
  (mq/init-connection connection-params)
  (mq/declare-and-bind-queues "in" "out")
  (doseq [m msgs] (do (mq/send-one "out" m)
                      (println "sent: " m))))

(defn receive-responses
  "Receives responses and writes them to file."
  [connection-params output-file]
  (mq/init-connection connection-params)
  (loop [msg (mq/get-one "in")]
    (if (nil? msg)
      (Thread/sleep 2000)
      (do (println "got msg: " msg)
          (with-out-append-writer output-file (prn-str msg))))
    (recur (mq/get-one "in"))))

(defn -main [& args]
  (with-command-line args "Parallel Clojush runs."
    [[host h "RabbitMQ host name." "127.0.0.1"]
     [port P "RabbitMQ port number." "5672"]
     [user u "RabbitMQ username." "guest"]
     [pass p "RabbitMQ password." "guest"]
     [distribute? d? "Distribute the parameters given by '--parameters'"]
     [parameters m "Clojure file providing a vector of parameter maps." "~/parameters.clj"]
     [output-file o "Outputs information about runs performed on the parameters given." "/home/brian/head-output"]
     [worker? w? "Process messages from queue."]
     [file f "Problem file to run."]
     [log-dir l "Worker output directory.  Can be the same for all workers." "~/worker-output"]]
     
     (let [connection-params {:host host :port (Integer. port) :user user :pass pass}]
       (cond distribute? (distribute connection-params (load-file parameters))
             worker? (w/start-worker connection-params "out" file log-dir)
             :else (receive-responses connection-params output-file))))
  (System/exit 0))
