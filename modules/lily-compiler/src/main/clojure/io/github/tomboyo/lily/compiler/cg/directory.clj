(ns io.github.tomboyo.lily.compiler.cg.directory
  (:require [io.github.tomboyo.lily.compiler.cg.helpers
             :as helpers
             :refer [map->ClassDef map->Method map->PackageDecl]]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast AstDirectory Fqn)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn static-factory [template]
  (let [type (ast/asType template)]
    (map->Method {:modifiers [:public :static]
                  :returns   type
                  :name      (.. template name typeName lowerCamelCase)
                  :body      [(str "return "
                                   (helpers/render type)
                                   ".new"
                                   (:name type)
                                   "();")]})))

(defn render [^AstDirectory directory]
  (Source.
    (.name directory)
    (helpers/render
      (let [type (ast/asType directory)]
        [(map->PackageDecl type)
         (map->ClassDef
           {:type type
            :body (map static-factory (.templates directory))})]))))

(comment
  (import '[io.github.tomboyo.lily.compiler.ast OperationParameter SimpleName AstTemplate ParameterLocation ParameterEncoding])
  (let [template (AstTemplate.
                   (.build (Fqn/newBuilder "com.example" "myOperation"))
                   [(OperationParameter. (SimpleName/of "id")
                                         "id"
                                         ParameterLocation/PATH
                                         (ParameterEncoding/simple)
                                         (.build (Fqn/newBuilder "java.lang" "String")))])
        directory (AstDirectory. (.build (Fqn/newBuilder "com.example" "Directory"))
                                 #{template})]
    (.contents (render directory)))
  )