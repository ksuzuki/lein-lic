(ns test-lic
  (:use [clojure.test]
        [clojure.contrib.java-utils :only [file delete-file]]
        [clojure.contrib.shell-out :only [with-sh-dir sh]]))

(defn same?
  [dir f1 f2]
  (with-sh-dir dir
    (let [ret (sh "cmp" "--quiet" f1 f2 :return-map true)]
      (if (zero? (:exit ret)) true false))))

(deftest test-lic-epl
  (let [dir "sample.epl"
        copying (file dir "COPYING")]
    ;;
    (when (.exists copying)
      (delete-file copying))
    ;;
    (with-sh-dir dir
      (sh "lein" "deps")
      (sh "lein" "lic"))
    ;;
    (is (.exists copying))
    (is (same? dir "COPYING" "../src/resources/epl-v10.txt"))
    ;;
    (is (same? dir "src/dregen.clj" (str "../test/" dir "/src/dregen.clj.epl")))
    (is (same? dir "src/dregen/generator.clj" (str "../test/" dir "/src/dregen/generator.clj.epl")))))

(deftest test-lic-gpl
  (let [dir "sample.gpl"
        copying (file dir "GPL")]
    ;;
    (when (.exists copying)
      (delete-file copying))
    ;;
    (with-sh-dir dir
      (sh "lein" "deps")
      (sh "lein" "lic"))
    ;;
    (is (.exists copying))
    (is (same? dir "GPL" "license/gpl.txt"))
    ;;
    (is (same? dir "src/dregen.clj" (str "../test/" dir "/src/dregen.clj.gpl")))
    (is (same? dir "src/dregen/generator.clj" (str "../test/" dir "/src/dregen/generator.clj.gpl")))
    (is (same? dir "src/html/dr.html" (str "../test/" dir "/src/html/dr.html.gpl")))))
