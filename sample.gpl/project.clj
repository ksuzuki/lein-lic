(defproject dregen "0.1.0"
  :description "Doctor regeneration project"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[org.clojars.ksuzuki/leiningen-lic "0.1.0"]]
  :lic {:artifact "dregen - Doctor regenerator"
        :author ["Romana"]
        :year [ "2011" ]
        :update true
        :file [license/gpl.txt "GPL"]
        :notice [license/notice.txt {:html ["<!--" "  -- " "  -->"]}]
        :dirs [src/html]}
  :namespaces [dregen
               dregen.generator]
  :main dregen)
