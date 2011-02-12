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

(ns lic.attachers
  (:import [java.io
            BufferedReader BufferedWriter File FileInputStream
            FileOutputStream InputStreamReader OutputStreamWriter
            StringReader StringWriter]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;; Refs

(def ->ftypes (ref {:clj ";; " :java ["/* " " * " " */"]}))
(def ->notice (ref ""))
(def ->notice4update (ref ""))
(def ->update (ref false))
(def ->scan-line (ref 100))
(def ->vcn-pattern (ref "Copyright\\s[\\s\\W\\n]*\\([Cc]\\)\\s[\\s\\W\\n]*([1-9]\\d{3,3}[.,-]?[\\s\\W\\n]*)+\\w+"))

(def ->status (ref false))
(def ->fixedcn (ref false))
(def ->debug (atom false))

;;;; Vars

(def *f* nil)
(def *bn* "%!")
(def *en* "!%")
(def *fc* "%%")
(def *cpr* "Copyright")
(def *cbn* (re-pattern (str "^\\W*\\s+" *bn* "|" *fc* "\\s+" *cpr*)))
(def *cen* (re-pattern (str "(All )?(rights )?reserved.\\s+" *en* "$")))
(def *cfc* (re-pattern (str "^\\W*\\s+" *fc* "\\s+" *cpr*)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn flatter
  "Flatten x"
  [x]
  (if (ns-resolve 'clojure.core 'flatten)
    ((find-var 'clojure.core/flatten) x)
    (filter (complement sequential?)
            (rest (tree-seq sequential? seq x)))))

(defn rety
  "Take x y and return y."
  [x y]
  y)

(defn deseq
  "Create a seq of dir-ext pairs per dir."
  [dirs exts]
  (lazy-seq (when (seq dirs)
              (concat (map (fn [d e] [d e])
                           (repeat (count exts) (first dirs))
                           exts)
                      (deseq (rest dirs) exts)))))

(defn find-files
  "Find all files having the extensions in the directories."
  ([dirs exts]
     (filter seq (map (fn [de] (find-files de)) (deseq dirs exts))))
  ([[dir ext]]
  (let [ptn (re-pattern (str "\\." (name ext) "$"))]
    (filter #(re-find ptn (str %)) (file-seq (File. dir))))))

(defn has-copyright-notice
  "Scan up to the specified lines or whole, looking for the copyright notice
   that matches to the pattern. Return values are:
   [:nw nil       ]   file is not writable
   [:nf nil       ]   copyright notice not found
   [:cn nil       ]   found copyright notice
   [:cc [bl el nl]]   found conforming copyright notice
   [:fc [bl el nl]]   found fixed conforming copyright notice
   bl := begin-line el := end-line nl := notice lines"
  []
  (if (not (.canWrite *f*))
    [:nw nil]
    (let [cmp (if (pos? @->scan-line) <= >=)
          rep (re-pattern @->vcn-pattern)
          cpr (re-pattern *cpr*)
          cft (fn [c n l]
                (let [c (if (and (nil? (first c)) (re-find *cbn* l))
                          [n nil (str l \newline)]
                          c)]
                  (if (nil? (second c))
                    (let [bl (first c)
                          nl (if (= bl n) (last c) (str (last c) l "\n"))]
                      [bl (when (re-find *cen* l) n) nl])
                    c)))]
      (with-open [rdr (BufferedReader. (InputStreamReader. (FileInputStream. *f*)))]
        (loop [lnum 1
               line (.readLine rdr)
               cnts nil
               conf [nil nil nil]]
          (if (and line (cmp lnum @->scan-line))
            (recur (inc lnum)
                   (.readLine rdr)
                   (if (or cnts (re-find cpr line))
                       (str cnts line "\n")
                       cnts)
                   (cft conf lnum line))
            (if (and cnts (re-find rep cnts))
              (if (number? (second conf))
                (if (re-find *cfc* (last conf))
                  [:fc conf]
                  [:cc conf])
                [:cn nil])
              [:nf nil])))))))

(defn create-temp-file
  []
  (File/createTempFile "lic" nil))

(defn file-extension
  []
  (let [file (.toString *f*)
        lidx (.lastIndexOf file (int \.))
        ext (.substring file (inc lidx))]
    ext))

(defn notice-format
  "Normalize start, prefix, and end in map."
  []
  (let [nf ((keyword (file-extension)) @->ftypes)]
    (if (instance? String nf)
      {:first nil :prefix nf :last nil}
      {:first (first nf) :prefix (second nf) :last (when (< 2 (count nf)) (nth nf 2))})))

(defn replace-target-file-with-temp-file
  "Try to preserve and restore the original file in case. In debug mode,
   original file isn't touched. Instead a copy will be created with .lic
   extension."
  [tmpf]
  (let [bak (File. (.getParent *f*) (str (.getName *f*) ".lbk"))
        dst (if @->debug
               (File. (.getParent *f*) (str (.getName *f*) ".lic"))
               *f*)]
    (try
     (when-not @->debug
       (.renameTo *f* bak))
     (.renameTo tmpf dst)
     (finally
      (when-not @->debug
        (if (.exists *f*)
          (.delete bak)
          (.renameTo bak *f*))))))
  tmpf)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn write-notice-to-temp-file
  [tmpf]
  (let [nf (notice-format)
        strm (FileOutputStream. tmpf)]
    (with-open [wtr (BufferedWriter. (OutputStreamWriter. strm))
                rdr (BufferedReader. (StringReader. @->notice))]
      (when-let [start (:first nf)]
        (.write wtr (str start "\n")))
      (let [prefix (:prefix nf)]
        (loop [line (.readLine rdr)]
          (when line
            (.write wtr (str prefix line "\n"))
            (recur (.readLine rdr)))))
      (when-let [end (:last nf)]
        (.write wtr (str end "\n")))
      ;;
      (.write wtr "\n")))
  tmpf)

(defn append-target-file-to-temp-file
  "Append target file to the temp file."
  [tmpf]
  (with-open [rdr (BufferedReader. (InputStreamReader. (FileInputStream. *f*)))
              wtr (BufferedWriter. (OutputStreamWriter. (FileOutputStream. tmpf true)))]
    (loop [line (.readLine rdr)]
      (when line
        (doto wtr
          (.write line)
          (.newLine))
        (recur (.readLine rdr)))))
  tmpf)

(defn attach-notice
  []
  (let [tmpf (create-temp-file)]
    (try
      (-> tmpf
          (write-notice-to-temp-file)
          (append-target-file-to-temp-file)
          (replace-target-file-with-temp-file))
      :attached
      (catch Exception e
       :failed)
      (finally ;; Make sure to remove temp file.
       (when (.exists tmpf)
         (.delete tmpf))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn same-notice?
  [ben]
  (let [nl (last ben)
        nf (notice-format)
        px (:prefix nf)
        sw (StringWriter.)]
    (with-open [rd (BufferedReader. (StringReader. @->notice4update))]
      (loop [ln (.readLine rd)]
        (when ln
          (.write sw (str px ln \newline))
          (recur (.readLine rd)))))
    (when @->debug
      (println "nl:\n" nl)
      (println "sw:\n" (.toString sw)))
    (= nl (.toString sw))))

(defn write-target-file-upto-notice
  [tmpf ben]
  (let [[bl el _] ben]
    (when (pos? (- bl 1))
      (with-open [rdr (BufferedReader. (InputStreamReader. (FileInputStream. *f*)))
                  wtr (BufferedWriter. (OutputStreamWriter. (FileOutputStream. tmpf)))]
        (loop [lnum 1
               line (.readLine rdr)]
          (when (< lnum bl)
            (doto wtr
              (.write line)
              (.newLine)))))))
  tmpf)

(defn append-updated-notice
  [tmpf]
  (let [nf (notice-format)
        prfx (:prefix nf)
        strm (FileOutputStream. tmpf true)]
    (with-open [wtr (BufferedWriter. (OutputStreamWriter. strm))
                rdr (BufferedReader. (StringReader. @->notice4update))]
      (loop [line (.readLine rdr)]
        (when line
          (doto wtr
            (.write (str prfx line))
            (.newLine))
          (recur (.readLine rdr))))))
  tmpf)

(defn append-rest-of-target-file
  [tmpf ben]
  (let [[bl el _] ben]
    (with-open [rdr (BufferedReader. (InputStreamReader. (FileInputStream. *f*)))
                wtr (BufferedWriter. (OutputStreamWriter. (FileOutputStream. tmpf true)))]
      (loop [lnum 1
             line (.readLine rdr)]
        (when line
          (if (<= lnum el)
            (recur (inc lnum) (.readLine rdr))
            (do
              (doto wtr
                (.write line)
                (.newLine))
              (recur (inc lnum) (.readLine rdr))))))))
  tmpf)

(defn update-notice
  [ben]
  (if (same-notice? ben)
    :exists
    (let [tmpf (create-temp-file)]
      (try
        (-> tmpf
            (write-target-file-upto-notice ben)
            (append-updated-notice)
            (append-rest-of-target-file ben)
            (replace-target-file-with-temp-file))
        :updated
        (catch Exception e
          :failed)
        (finally ;; Make sure to remove temp file.
         (when (.exists tmpf)
           (.delete tmpf)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn attacher
  "Return an array of the file being processed and its status keyword paired."
  [file]
  (future
   (binding [*f* file]
     (let [[stat ben] (has-copyright-notice)]
       (when @->debug
         (println "file:" file "stat:" stat "ben:" ben))
       [*f* (cond
             (= stat :nw) :unreadwritable
             (= stat :cn) :exists
             (= stat :cc) (if @->update (update-notice ben) :exists)
             (= stat :fc) :fixedcn
             :else (attach-notice))]))))

(defn eval-attacher-results
  [vals]
  (binding [*out* *err*]
    (doseq [v vals]
      (let [file (str (first v))
            stat (second v)]
        (cond
         (= stat :exists)
           ;; A copyright notice exists already.
           (when @->status (println "   " file))
         (= stat :attached)
           ;; A copyright notice is attached (and saved with .lic in the debug mode).
           (when @->status (println "A  " file))
         (= stat :updated)
           ;; Copyright notice is updated (and saved with .lic in the debug mode).
           (when @->status (println "U  " file))
         (= stat :fixedcn)
           ;; Conforming notice is fixed.
           (when (or @->status @->fixedcn) (println "F  " file))
         (= stat :unreadwritable)
           ;; File is not writable.
           (println "W! " file)
         (= stat :failed)
           ;; Error - failed to process the file
           (println "E! " file)
         :else
           ;; Faital - unknown attacher status
           (println "F! " file))))))

(defn start-attachers
  "Create attachers as future objects and then derefer their return values.
   That should go like the notice is attached to the files concurrently, yet
   this fn doesn't return until all future threads finish."
  [licm notice notice4update options]
  (dosync
   (alter ->notice rety notice)
   (alter ->notice4update rety notice4update)
   (alter ->update rety (:update licm))
   (alter ->ftypes rety (second (:notice licm)))
   (alter ->vcn-pattern rety (:vcn-pattern licm))
   (alter ->scan-line rety (:scan-line licm))
   (alter ->status rety (:status options))
   (alter ->fixedcn rety (:fixedcn options)))
  ;;
  (let [dirs (:dir licm)
        exts (keys (second (:notice licm)))
        vals (map deref (map #(attacher %) (flatter (find-files dirs exts))))]
    (eval-attacher-results vals)))
