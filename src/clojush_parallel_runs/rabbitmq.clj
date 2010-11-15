(ns clojush-parallel-runs.rabbitmq
  (:use [rabbitcj.client]
        [clj-serializer.core]))

(def chan nil)

(defn init-connection
  "needs to be done once before other functions are used"
  [{:keys [host port user pass]}]
  (def chan (-> {:username user :password pass
                 :virtual-host "/" :host host :port port}
              (connect)
              (create-channel))))

(defn declare-and-bind-queues
  [& queue-names]
  (doseq [queue-name queue-names]
    (declare-exchange chan queue-name fanout-exchange false false nil)
    (declare-queue chan queue-name false false true nil)
    (bind-queue chan queue-name queue-name "")))

(defn get-one [q] (try (deserialize (.. chan (basicGet q true) (getBody)) (Object.))
                    (catch java.lang.Exception _ nil)))

(defn send-one [q i] (publish chan q "" (String. (serialize i))))
