(ns io.github.tomboyo.lily.compiler.cg.template
  (:require [clojure.string :as str]
            [io.github.tomboyo.lily.compiler.cg.helpers
             :as helpers
             :refer [map->PackageDecl map->Record map->Method map->Type map->Field]]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast
             AstTemplate Fqn OperationParameter ParameterEncoding
             ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn parameterRecordStaticFactory
  [type fields]
  (helpers/staticFactory type fields (fn [_] "null")))

(defn templateStaticFactory
  [type fields]
  (helpers/staticFactory type fields #(str (helpers/render (:type %))
                                           "." helpers/emptyFactoryName "()"))
  )

(defn pathParameters [template]
  (let [type (map->Type {:name "PathParameters"})
        fields (map ast/asField (.pathParameters template))]
    (map->Record
      {:type   type
       :fields fields
       :body   [(parameterRecordStaticFactory type fields)]
       :meta #{:withers}})))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (helpers/render
             (let [type (ast/asType astTemplate)
                   pathParameters (pathParameters astTemplate)
                   fields [(helpers/toField pathParameters)]]
               [(map->PackageDecl type)
                (map->Record {:type   type
                              :fields fields
                              :body   [(templateStaticFactory type fields)
                                       pathParameters]
                              :meta   #{:fwithers}})]))))

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