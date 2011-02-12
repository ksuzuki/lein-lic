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

(ns lic.license
  (:import [java.io
            BufferedReader BufferedWriter File FileInputStream FileOutputStream
            InputStreamReader OutputStreamWriter]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn source-filepath
  [licm]
  (if (= (first (:file licm)) ":default")
    "default"
    (first (:file licm))))

(defn source-reader
  "Read from the embedded epl-v10.txt by default."
  [licm project]
  (BufferedReader.
   (let [sfpath (source-filepath licm)]
     (InputStreamReader.
      (if (= sfpath "default")
        (ClassLoader/getSystemResourceAsStream "resources/epl-v10.txt")
        (FileInputStream. (File. (str (:root project)) sfpath)))))))

(defn destination-file
  [licm project]
  (File. (str (:root project)) (second (:file licm))))

(defn destination-writer
  "Write to the destination file."
  [licm project]
  (BufferedWriter. (OutputStreamWriter. (FileOutputStream. (destination-file licm project)))))

(defn print-license
  [licm project]
  (println "[ License:" (source-filepath licm) "]")
  (with-open [rdr (source-reader licm project)]
    (loop [line (.readLine rdr)]
      (when line
        (println line)
        (recur (.readLine rdr))))))

(defn attach-license
  [licm project preview _]
  (if preview
    (print-license licm project)
    (when-not (.exists (destination-file licm project))
      (with-open [rdr (source-reader licm project)
                  wtr (destination-writer licm project)]
        (loop [line (.readLine rdr)]
          (when line
            (doto wtr
              (.write line)
              (.newLine))
            (recur (.readLine rdr))))))))
