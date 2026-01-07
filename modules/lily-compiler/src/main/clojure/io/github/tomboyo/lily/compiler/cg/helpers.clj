(ns io.github.tomboyo.lily.compiler.cg.helpers
  (:require [clojure.string :as str])
  (:import (clojure.lang Keyword Seqable)))

(defn indent [s]
  (str/join (map #(str "  " %) (str/split-lines s))))

(defprotocol Render
  (render [x]))

(extend-protocol Render
  nil
  (render [_] "")

  String
  (render [s] s)

  Keyword
  (render [k] (subs (str k) 1))

  Seqable
  (render [xs] (str/join "\n" (map render xs)))
  )

(defrecord Record [type]
  Render
  (render [_]
    (str/join "\n" [(str "package " (:package type) ";")
                    (str "public record " (:name type) "() {")
                    "}"])))

(defrecord ClassDef [type body]
  Render
  (render [_]
    (str/join "\n" [(str "package " (:package type) ";")
                    (str "public class " (:name type) " {")
                    (-> body render indent)
                    "}"])))

(defrecord Method [modifiers returns name body]
  Render
  (render [_]
    (str/join "\n"
              [(str (str/join " " (map render modifiers))
                    " "
                    (render returns)
                    " "
                    name
                    "() {")
               (-> body render indent)
               "}"])))

(defrecord Type [package name]
  Render
  (render [_] (str package "." name)))

(comment
  (render (map->Record {:type {:package "com.example.template"
                               :name    "MyRecord"}}))
  (render (let [return-type (map->Type {:package "com.example.template"
                                        :name    "GetFooTemplate"})
                method (map->Method {:modifiers   [:public :static]
                                     :return-type return-type
                                     :name        "getFoo"
                                     :body        [(str "return new " (render return-type) "();")]
                                     })
                class-def (map->ClassDef {:type {:package "com.example" :name "Directory"}
                                          :body [method]})]
            class-def)))