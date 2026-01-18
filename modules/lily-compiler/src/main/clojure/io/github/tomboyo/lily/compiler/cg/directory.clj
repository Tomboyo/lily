(ns io.github.tomboyo.lily.compiler.cg.directory
  (:require [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast]
            [io.github.tomboyo.lily.compiler.cg.string-template :as st])
  (:import (io.github.tomboyo.lily.compiler.ast AstDirectory Fqn)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn render [^AstDirectory directory]
  (Source.
    (.name directory)
    (.render (st/directory
             {:type      (ast/asType directory)
              :templates (map ast/asType (.templates directory))}))))

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
    (.contents (render directory))
    )
  )