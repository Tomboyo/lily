(ns io.github.tomboyo.lily.compiler.cg.interop.ast
  (:import (io.github.tomboyo.lily.compiler.ast Ast Fqn OperationParameter)))

(defn asType [x]
  (condp instance? x
    Fqn
    {:package    (.. x packageName toString)
     :name       (.. x typeName upperCamelCase)
     :parameters (map asType (.typeParameters x))}

    OperationParameter (asType (.typeName x))

    Ast (asType (.name x))))

(defn asField [^OperationParameter x]
  {:type (asType x)
   :name (.lowerCamelCase (.name x))})
