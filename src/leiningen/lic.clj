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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; license key-value syntax
;;   :artifact artifact-name
;;   :author [ "NAME"+ ]
;;   :year [ year-spec+ ]
;;   :update true | false
;;   :wrap-at POSITIVE-INTEGER
;;   :dir [ dir-spec+ ]
;;   :file [ file-spec ]
;;   :notice [ notice-spec ]
;;   :scan-line ZERO-OR-POSITIVE-INTEGER
;;   :vcn-pattern "REGEXP"
;;       :debug mode-switch
;;
;;   artifact-name  ::= "ARTIFACTNAME" | :description
;;   year-spec      ::= YEAR-NUMBER | year-string
;;   year-string    ::= "YEAR" | "YEAR1, YEAR2, ..." | "YEAR1-YEAR2"
;;   dir-spec       ::= "RELATIVE PATH/FROM/PROJECT ROOT"
;;   file-spec      ::= [ file-path "FILENAME"? ]
;;   file-path      ::= :default | "RELATIVE PATH/FROM/PROJECT ROOT"
;;   notice-spec    ::= [ file-path file-types* ]
;;   file-types     ::= { file-type+ }
;;   file-type      ::= ext-keyword format
;;   format         ::= "PREFIX" | [ start-line prefix end-line? ]
;;   ext-keyword    ::= :clj | :java | :USER-DEFINED*
;;   start-line     ::=        "/*"  | "USER-DEFINED"*
;;   prefix         ::= "; " | " * " | "USER-DEFINED"*
;;   end-line       ::=        " */" | "USER-DEFINED"*
;;   mode-switch    ::= false | true

(ns ^{:doc "Attach license and copyright notice."
      :author "Kei Suzuki"}
    leiningen.lic
  (:use [clojure.stacktrace :only (print-cause-trace)]
        [lic.license :only (attach-license)]
        [lic.notice :only (attach-notice *wrap-at*)]
        [lic.attachers :only (->scan-line ->vcn-pattern ->debug)])
  (:import [java.util
            Calendar]))

(def *lic-version* "1.0.0")

(defn get-options
  "Return lic options in map, including unknown option with :unknown."
  ([opts]
     (get-options {} opts))
  ([m opts]
     (if (seq opts)
       (let [op (str (first opts))]
         (cond
          (= op "f") (recur (conj m {:fixedcn true}) (rest opts))
          (= op "p") (recur (conj m {:preview true}) (rest opts))
          (= op "s") (recur (conj m {:status true}) (rest opts))
          (= op "?") (recur (conj m {:help true}) (rest opts))
          :else (recur (conj m {:unknown true}) (rest opts))))
       m)))

(defn print-lic-version
  []
  (println "lic version" *lic-version* "\n"))

(defn print-lic-key-value-syntax
  []
  (print (str
          "lic key-value syntax\n"
          "   :artifact artifact-name\n"
          "   :author [ \"NAME\"+ ]\n"
          "   :year [ year-spec+ ]\n"
          "   :update true | false\n"
          "   :wrap-at POSITIVE-INTEGER\n"
          "   :dir [ dir-spec+ ]\n"
          "   :file [ file-spec ]\n"
          "   :notice [ notice-spec ]\n"
          "   :scan-line ZERO-OR-POSITIVE-INTEGER\n"
          "   :vcn-pattern \"REGEXP\"\n"
          "\n"
          "   artifact-name  ::= \"ARTIFACTNAME\" | :description\n"
          "   year-spec      ::= YEAR-NUMBER | year-string\n"
          "   year-string    ::= \"YEAR\" | \"YEAR1, YEAR2, ...\" | \"YEAR1-YEAR2\"\n"
          "   dir-spec       ::= \"RELATIVE PATH/FROM/PROJECT ROOT\"\n"
          "   file-spec      ::= [ file-path \"FILENAME\"? ]\n"
          "   file-path      ::= :default | \"RELATIVE PATH/FROM/PROJECT ROOT\"\n"
          "   notice-spec    ::= [ file-path file-types* ]\n"
          "   file-types     ::= { file-type+ }\n"
          "   file-type      ::= ext-keyword format\n"
          "   format         ::= \"PREFIX\" | [ start-line prefix end-line? ]\n"
          "   ext-keyword    ::= :clj | :java | :USER-DEFINED*\n"
          "   start-line     ::=        \"/*\"  | \"USER-DEFINED\"*\n"
          "   prefix         ::= \"; \" | \" * \" | \"USER-DEFINED\"*\n"
          "   end-line       ::=        \" */\" | \"USER-DEFINED\"*\n"
          "\n"
          "   Note: set :scan-line to 0 to scan whole lines\n")))

(defmacro get-lic-map
  []
  `(:lic ~'project))

(defn artifact
  [project]
  (let [av (or (:artifact (get-lic-map))
               (symbol (:group project) (:name project)))]
    (if (= av :description)
      (if-let [desc (av project)]
        (str desc)
        (throw (Exception. "no artifact name found")))
      (str av))))

(defn author
  [project]
  (if-let [author (:author (get-lic-map))]
    (if (and (vector? author) (every? string? author))
      author
      (throw (Exception. "invalid author name(s)")))
    [(str (System/getProperty "user.name"))]))

(defn year
  [project]
  (if-let [year (:year (get-lic-map))]
    (if (and (vector? year) (every? (fn [y] (or (string? y) (and (number? y) (pos? y)))) year))
      (apply vector (map str year))
      (throw (Exception. "invalid year spec")))
    [(str (.get (Calendar/getInstance) Calendar/YEAR))]))

(defn fspec
  [project]
  (if-let [fspec (:file (get-lic-map))]
    (if (and (seq fspec) (< (count fspec) 3))
      (let [[fpath fname] fspec]
        [(str fpath) (if (nil? fname) "COPYING" (str fname))])
      (throw (Exception. "invalid file spec")))
    [":default" "COPYING"]))

(defn default-file-types
  []
  {:clj ";; " :java ["/*" " * " " */"]})

(defn file-types
  "Return file-types with easy syntax check"
  [notice-spec]
  (if (and (seq notice-spec) (seq (second notice-spec)) (< (count notice-spec) 3))
    (let [ftm (reduce (fn [m [x fmt]]
                        (if (keyword? x)
                          (cond
                           (instance? String fmt)
                             (assoc m x fmt)
                           (instance? clojure.lang.PersistentVector fmt)
                             (if (< (count fmt) 4)
                               (let [[s p e] fmt]
                                 (cond (and s p e) (assoc m x [s p e])
                                       (and s p) (assoc m x [s p])
                                       :else (assoc m :invalid x)))
                               (assoc m :invalid x))
                           :else
                             (assoc m :invalid x))
                          (assoc m :invalid (str x))))
                      (default-file-types)
                      (second notice-spec))]
      (if (:invalid ftm)
        (throw (Exception. (str "invalid file type spec for " (:invalid ftm))))
        ftm))
    (throw (Exception. "invalid notice spec"))))

(defn notice
  "Return notice-spec in vector. :clj and :java file-types are always icnluded,
   and their default formats may be overridden."
  [project]
  (if-let [notice-spec (:notice (get-lic-map))]
    (if (and (seq notice-spec) (< 1 (count notice-spec)))
      [(str (first notice-spec)) (file-types notice-spec)]
      [(str (first notice-spec)) (default-file-types)])
    [":default" (default-file-types)]))

(defn update
  "Do not update by default."
  [project]
  (if-let [update (:update (get-lic-map))]
    (if (or (true? update) (false? update))
      update
      (throw (Exception. ":update value has to be either true or false")))
    false))

(defn dir
  "src/ is always included by default."
  [project]
  (if-let [dir (:dir (get-lic-map))]
    (if (and (vector? dir) (every? string? dir))
      (apply conj []
             (str (or (:source-path project) "src"))
             (map #(str (:root project) "/" %) dir))
      (throw (Exception. "invalid dir spec")))
    [(str (or (:source-path project) "src"))]))

(defn scan-line
  [project]
  (let [num (or (:scan-line (get-lic-map)) @->scan-line)]
    (if (and (<= 0 num) (instance? Integer num))
      num
      (throw (Exception. "scan line number must be >=0")))))

(defn vcn-pattern
  [project]
  (str (or (:vcn-pattern (get-lic-map)) @->vcn-pattern)))

(defn wrap-at
  [project]
  (let [num (or (:wrap-at (get-lic-map)) *wrap-at*)]
    (if (and (pos? num) (instance? Integer num))
      num
      (throw (Exception. "wrap column number must be >0")))))

(defn debug-lic?
  [project]
  (if (contains? (get-lic-map) :debug)
    (swap! ->debug (fn [_ y] y) (:debug (get-lic-map)))
    @->debug))

(defn licm
  "(Re)construct lic map."
  [project]
  {:artifact    (artifact project)
   :author      (author project)
   :year        (year project)
   :update      (update project)
   :wrap-at     (wrap-at project)
   :dir         (dir project)
   :file        (fspec project)
   :notice      (notice project)
   :scan-line   (scan-line project)
   :vcn-pattern (vcn-pattern project)})

(defn print-lic
  [licm]
  (let [charset (if (= (:charset licm) "") "(default)" (:charset licm))]
    (println "lic key-values")
    (println "  :artifact    " (pr-str (:artifact licm)))
    (println "  :author      " (pr-str (:author licm)))
    (println "  :year        " (pr-str (:year licm)))
    (println "  :update      " (pr-str (:update licm)))
    (println "  :wrap-at     " (:wrap-at licm))
    (println "  :dir         " (pr-str (:dir licm)))
    (println "  :file        " (pr-str (:file licm)))
    (println "  :notice      " (pr-str (:notice licm)))
    (println "  :scan-line   " (:scan-line licm))
    (println "  :vcn-pattern " (pr-str (:vcn-pattern licm)))))

(defn print-help
  [licm]
  (print-lic-version)
  (print-lic-key-value-syntax)
  (println)
  (print-lic licm))

(defn process
  "Run license and copyright notice attach processes."
  [licm project preview options]
  (attach-license licm project preview options)
  (when preview (println "\n"))
  (attach-notice licm project preview options))

(defn lic
  "Create a license file at the project's root directory, attach copyright
notice to source file and update existing one. An option is either:
  f  print file with fixed conforming copyright notice only
  p  preview license and copyright notice
  s  print processing status
  ?  print lic version, key-value syntax and current values"
  [project & option]
  (when (debug-lic? project)
    (println "lic in debug mode"))
  (try
    (let [licm (licm project)
          options (get-options option)]
      (when (:unknown options)
        (throw (Exception. "unknown option")))
      (if (:help options)
        (print-help licm)
        (process licm project (:preview options) options)))
    (catch Exception e
      (binding [*out* *err*]
        (println "abort lic:" (.getMessage e))
        (when @->debug
          (print-cause-trace e)
          (.flush *out*)))))
  1)
