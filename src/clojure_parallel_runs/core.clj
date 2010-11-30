(ns clojure-parallel-runs.core
  (:use [clojure-parallel-runs.rabbitmq :as mq]
        [clojure-parallel-runs.worker :as w]
        [clojure.contrib.duck-streams :only [with-out-append-writer]]
        [clojure.contrib.command-line])
  (:gen-class))

(defn distribute
  "Distributes a sequence of objects
  (map, vector, seq, java obj, whatever)."
  [connection-params q-out q-in msgs]
  (mq/init-connection connection-params)
  (mq/declare-and-bind-queues q-in q-out)
  (doseq [m msgs] (do (mq/send-one q-out m)
                      (println "sent: " m))))

(defn receive-responses
  "Receives responses and writes them to file."
  [connection-params q-in output-file]
  (mq/init-connection connection-params)
  (loop [msg (mq/get-one q-in)]
    (if (nil? msg)
      (Thread/sleep 2000)
      (do (println "got msg: " msg)
          (with-out-append-writer output-file (prn msg))))
    (recur (mq/get-one q-in))))

(defn clear
  "Empties the given queues"
  [connection-params & queues]
  (doseq [q queues]
    (dotimes [i 10000]
      (mq/get-one q))))

(defn -main [& args]
  (with-command-line args "Parallel Clojush runs."
    [[distribute? d? "Distribute the parameters given by '--parameters'"]
     [parameters m "Clojure file providing a vector of parameter maps." "~/parameters.clj"]
     [queue-name q "A distinct queue name for distribution and collection." "default"]
     [output-file o "Outputs information about runs performed on the parameters given." "~/output"]
     [worker? w? "Process messages from queue."]
     [clear? c? "Clears the specified queue of all messages."]
     [file f "Problem file to run."]
     [log-dir l "Worker log directory.  Can be the same for all workers." nil]
     [host h "RabbitMQ host name." "127.0.0.1"]
     [port P "RabbitMQ port number." "5672"]
     [user u "RabbitMQ username." "guest"]
     [pass p "RabbitMQ password." "guest"]]

     (let [connection-params {:host host :port (Integer. port) :user user :pass pass}
           q-in (str queue-name "-in")
           q-out (str queue-name "-out")]
       (cond distribute? (distribute connection-params q-out q-in (load-file parameters))
             worker? (w/start-worker connection-params q-out q-in file log-dir)
             clear? (clear connection-params q-out q-in)
             :else (receive-responses connection-params q-in output-file))))
  (System/exit 0))
