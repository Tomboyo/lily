(ns io.github.tomboyo.lily.compiler.cg.template
  (:require [io.github.tomboyo.lily.compiler.cg.helpers
             :as helpers
             :refer [map->PackageDecl map->Record]]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast
             AstTemplate Fqn OperationParameter ParameterEncoding
             ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn pathParameters [template]
  (map->Record
    {:type   {:name "PathParameters"}
     :fields (map ast/asField (.pathParameters template))}))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (helpers/render
             (let [type (ast/asType astTemplate)]
               [(map->PackageDecl type)
                (map->Record {:type type
                              :body (pathParameters astTemplate)})]))))

(comment
  (import [io.github.tomboyo.lily.compiler.ast SimpleName ParameterLocation ParameterEncoding])
  (.contents (render (AstTemplate.
                       (.build (Fqn/newBuilder "com.example" "myOperation"))
                       [(OperationParameter. (SimpleName/of "id")
                                             "id"
                                             ParameterLocation/PATH
                                             (ParameterEncoding/simple)
                                             (.build (Fqn/newBuilder "java.lang" "String")))])))
  )