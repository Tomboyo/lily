(ns io.github.tomboyo.lily.compiler.cg.interop.ast
  (:import (io.github.tomboyo.lily.compiler.ast Fqn)))

(defn asType [^Fqn fqn]
  {:package    (.. fqn packageName toString)
   :name       (.. fqn typeName upperCamelCase)
   :parameters (map asType (.typeParameters fqn))})
