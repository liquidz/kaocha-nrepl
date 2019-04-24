(ns kaocha-nrepl.test-helper
  (:require [kaocha-nrepl.core :as core]
            [nrepl.core :as nrepl]
            [nrepl.server :as server]))

(defmacro with-test-server [[session-sym] & body]
  `(let [server# (server/start-server
                  :handler (server/default-handler #'core/wrap-kaocha))
         transport# (nrepl/connect :port (:port server#))
         client# (nrepl/client transport# Long/MAX_VALUE)
         ~session-sym (nrepl/client-session client#)]
     (try
       ~@body
       (finally
         (.close transport#)
         (.close server#)))))

(defn random-id []
  (str (java.util.UUID/randomUUID)))

(defn send-message [session msg]
  (->> (merge {:id (random-id)} msg)
       (nrepl/message session)))
