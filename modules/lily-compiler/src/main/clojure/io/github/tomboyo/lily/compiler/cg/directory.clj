(ns io.github.tomboyo.lily.compiler.cg.directory
  (:require [clojure.java.io :as io])
  (:import (io.github.tomboyo.lily.compiler.ast AstDirectory)
           (io.github.tomboyo.lily.compiler.cg Mustache Source)))

(def template (slurp (io/resource "templates/directory.java.mustache")))

(defn render [^AstDirectory templates]
  (Source. (.name templates)
           (Mustache/writeString
             template
             (str ::render)
             {
              "packageName" (.packageName (.name templates))
              "className"   (.typeName (.name templates))
              "template"    (eduction (map #(hash-map "returnType" (.toFqpString (.name %))
                                                      "methodName" (.lowerCamelCase (.typeName (.name %)))
                                                      "constructor" (.toFqpString (.name %))))
                                      (.templates templates))
              })))
