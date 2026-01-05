(ns io.github.tomboyo.lily.compiler.cg.helpers
  (:require [clojure.string :as str])
  (:import (clojure.lang Keyword)))

(defn indent [s]
  (if (seq? s)
    (map #(str "  " %) s)
    (str "  " s)))

(defprotocol Render
  (render [x]))

(defn render-str [x]
  (let [x (render x)]
    (cond
      (coll? x) (str/join "\n" x)
      (string? x) x)))

(extend-protocol Render
  nil
  (render [_] "")

  String
  (render [s] s)

  Keyword
  (render [k] (subs (str k) 1))
  )

(defrecord Record [type]
  Render
  (render [_]
    (concat
      [(str "package " (:package type) ";")
       (str "public record " (:name type) "() {")]
      ["}"])))

(defrecord ClassDef [type body]
  Render
  (render [_]
    (concat
      [(str "package " (:package type) ";")
       (str "public class " (:name type) " {")]
      (flatten (map (comp indent render) body))
      ["}"])))

(defrecord Method [modifiers returns name body]
  Render
  (render [_]
    (concat
      [(str (str/join " " (map render modifiers))
            " "
            (render returns)
            " "
            name
            "() {")]
      (flatten (map (comp indent render) body))
      ["}"])))

(defrecord Type [package name]
  Render
  (render [_] (str package "." name)))

(comment
  (render (map->Record {:type {:package "com.example.template"
                               :name "MyRecord"}}))
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