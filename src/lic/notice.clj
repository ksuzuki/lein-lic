;; %! Copyright (C) 2011 Kei Suzuki  All rights reserved. !%
;; 
;; This file is part of lic - a leiningen license attacher plugin ("This
;; Software").
;; 
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License version 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns lic.notice
  (:use [lic.attachers :only (*bn* *en* start-attachers flatter)])
  (:import [java.io
            BufferedReader BufferedWriter File FileInputStream
            InputStreamReader OutputStreamWriter StringReader StringWriter]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def *wrap-at* 74)

(def *year* "")
(def *author* "")
(def *artifact* "")
(def *copying* "")

(def *notice-for-update*
  [:bn "Copyright" "(C)" :year :author " All" "rights" "reserved." :en "\n"])

(def *notice-contents*
  (concat *notice-for-update*
   ["\n"
    "This" "file" "is" "part" "of" :artifact "(\"This" "Software\")." "\n"
    "\n"
    "The" "use" "and" "distribution" "terms" "for" "this" "software"
    "are" "covered" "by" "the" "Eclipse" "Public" "License" "version"
    "1.0" "(http://opensource.org/licenses/eclipse-1.0.php)" "which"
    "can" "be" "found" "in" "the" :copying "at" "the" "root" "of" "this"
    "distribution." "\n"
    "By" "using" "this" "software" "in" "any" "fashion," "you" "are"
    "agreeing" "to" "be" "bound" "by" "the" "terms" "of" "this"
    "license." "\n"
    "You" "must" "not" "remove" "this" "notice," "or" "any" "other,"
    "from" "this" "software." "\n"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-lines
  [line len words]
  (if (seq words)
    (let [word (first words)]
      (if (= word "\n")
        (recur (str line "\n") 0 (rest words))
        (if (< *wrap-at* (+ len (count word)))
          (recur (str line "\n") 0 words)
          (recur (str line (when-not (zero? len) " ") word)
                 (+ len (if (zero? len) 0 1)
                    (count word)) (rest words)))))
    line))
                 
(defn interpose-cp
  [col]
  (map (fn [i s] (str i s))
       col
       (concat (repeat (dec (count col)) ",") '(""))))

(defn notice-keyword-to-string
  [s]
  (cond
   (= s :bn) *bn*
   (= s :en) *en*
   (= s :year) *year*
   (= s :author) *author*
   (= s :artifact) *artifact*
   (= s :copying) *copying*
   :else s))

(defn default-notice-content
  [licm]
  (binding [*year* (interpose-cp (:year licm))
            *author* (interpose-cp (:author licm))
            *artifact* (into [] (.split (str (:artifact licm)) "\\s"))
            *copying* (second (:file licm))]
    (doall (flatter (map notice-keyword-to-string *notice-contents*)))))

(defn default-notice
  "Create a default copyright notice, wrapping each line at a column nicely."
  [licm]
  (wrap-lines "" 0 (default-notice-content licm)))

(defn notice
  [licm project]
  (let [fpath (first (:notice licm))]
    (if (= fpath ":default")
      (default-notice licm)
      (let [fpath (str (:root project) "/" fpath)]
        (slurp fpath)))))

(defn notice-for-update
  [licm project]
  (binding [*year* (interpose-cp (:year licm))
            *author* (interpose-cp (:author licm))]
    (wrap-lines "" 0 (doall (flatter (map notice-keyword-to-string *notice-for-update*))))))

(defn print-notice
  [licm project]
  (let [fpath (first (:notice licm))
        from (if (= fpath ":default") "default" fpath)]
    (println "[ Copyright notice:" from "]")
    (with-open [rdr (BufferedReader. (StringReader. (notice licm project)))]
      (loop [line (.readLine rdr)]
        (when line
          (println ";" line)
          (recur (.readLine rdr)))))))
   
(defn attach-notice
  "For convenience, bind column for this thread in preview. Attaching the
   notice is taken care of by the attachers."
  ([licm project preview options]
     (binding [*wrap-at* (:wrap-at licm)]
       (if preview
         (print-notice licm project)
         (attach-notice licm project options))))
  ([licm project options]
     (start-attachers licm (notice licm project) (notice-for-update licm project) options)))
