(ns io.github.tomboyo.lily.compiler.cg.interop.ast
  (:require [io.github.tomboyo.lily.compiler.cg.helpers :refer [map->Type]])
  (:import (io.github.tomboyo.lily.compiler.ast Ast Fqn)))

(defn asType [x]
  (condp instance? x
    Fqn
    (map->Type {:package    (.. x packageName toString)
                :name       (.. x typeName upperCamelCase)
                :parameters (map asType (.typeParameters x))})

    Ast (asType (.name x))))
