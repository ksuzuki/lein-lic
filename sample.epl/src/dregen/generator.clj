(ns dregen.generator)

(defn regen
  [part]
  (cond (= part :head)  :body
        (= part :body)  :hands
        (= part :hands) :legs
        (= part :legs)  :done
        :else :fail))
