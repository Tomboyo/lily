(ns io.github.tomboyo.lily.compiler.cg.template
  (:require [io.github.tomboyo.lily.compiler.cg.helpers :as helpers]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast-interop])
  (:import (io.github.tomboyo.lily.compiler.ast AstTemplate Fqn OperationParameter ParameterEncoding ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (helpers/render
             (helpers/map->Record {:type (ast-interop/asType (.name astTemplate))}))))

(comment
  (import [io.github.tomboyo.lily.compiler.ast SimpleName ParameterLocation ParameterEncoding])
  (render (AstTemplate. (.build (Fqn/newBuilder "com.example" "myOperation"))
                        [(OperationParameter. (SimpleName/of "id")
                                              "id"
                                              ParameterLocation/PATH
                                              (ParameterEncoding/simple)
                                              (.build (Fqn/newBuilder "java.lang" "String")))]))
  )