(ns io.github.tomboyo.lily.compiler.cg.template
  (:require [clojure.java.io :as io])
  (:import (io.github.tomboyo.lily.compiler.ast AstTemplate Fqn)
           (io.github.tomboyo.lily.compiler.cg Mustache Source)))

(def template (slurp (io/resource "templates/template.java.mustache")))

; TODO: this creates stubs.
(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (Mustache/writeString
             template
             (str ::render)
             {
              "packageName" (.packageName (.name astTemplate))
              "className"   (.upperCamelCase (.typeName (.name astTemplate)))
              })))

(comment
  (render (AstTemplate. (.build (Fqn/newBuilder "com.example" "myOperation"))))
  )