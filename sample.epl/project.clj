(defproject dregen "0.1.0"
  :description "Doctor regeneration project"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[lein-lic "1.0.0"]]
  :lic {:artifact "dregen - Doctor regenerator"
        :author ["Romana"]}
  :namespaces [dregen
               dregen.generator]
  :main dregen)
