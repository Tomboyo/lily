(ns io.github.tomboyo.lily.compiler.cg.template
  (:require [clojure.string :as str]
            [io.github.tomboyo.lily.compiler.cg.helpers
             :as helpers
             :refer [map->PackageDecl map->Record map->Method map->Type]]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast
             AstTemplate Fqn OperationParameter ParameterEncoding
             ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn parameterRecordStaticFactory
  [type fields]
  (let [name (:name type)]
    (map->Method {:modifiers [:public :static]
                  :returns   type
                  :name      (str "new" name)
                  :body      [(str "return new " (helpers/render type) "("
                                   (str/join ", " (map (fn [_] "null") fields))
                                   ");")]
                  })))

(defn pathParameters [template]
  (let [type (map->Type {:name "PathParameters"})
        fields (map ast/asField (.pathParameters template))]
    (map->Record
      {:type   type
       :fields fields
       :body [(parameterRecordStaticFactory type fields)]})))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (helpers/render
             (let [type (ast/asType astTemplate)]
               [(map->PackageDecl type)
                (map->Record {:type type
                              :body [(pathParameters astTemplate)]})]))))

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