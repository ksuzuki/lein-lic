(defproject lein-lic "1.0.0"
  :description "lein-lic - a leiningen license attacher plugin"
  :lic {:artifact :description
        :author ["Kei Suzuki"]
        :update true}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
;  :aot [leiningen.lic
;        lic.attachers lic.license lic.notice]
;  :dev-dependencies [[lein-lic "1.0.0"]]
  :eval-in-leiningen true)
