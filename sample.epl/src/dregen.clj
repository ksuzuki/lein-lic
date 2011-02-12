(ns dregen
  (:gen-class)
  (:use dregen.generator))

(defn k9-affirmative
  [fun]
  (println "k9: affirmative")
  (fun))

(defn k9-negative
  [cmd tgt]
  (println "k9: negative. unknown command/target:" cmd tgt))

(defn k9-report
  [msg & status]
  (println "k9:" msg)
  (when (false? (first status))
    (println "k9: I am sorry")))

(defn regen-doctor
  []
  (let [part (if (< (* (rand) 10) 8) :head :hair)]
    (loop [status (regen part)]
      (cond
       (= status :done) (k9-report "regeneration of docutor completed")
       (= status :fail) (k9-report "regenerating doctor failed" false)
       :else (recur (regen status))))))

(defn k9
  [command target]
  (cond
   (and (= command :regenerate) (= target :doctor)) (k9-affirmative regen-doctor)
   :else (k9-negative command target)))

(defn dregen
  []
  (println "regenerate doctor, k9!")
  (k9 :regenerate :doctor))

(defn -main
  [& argv]
  (dregen))
