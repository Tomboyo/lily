(ns io.github.tomboyo.lily.compiler.cg.template
  (:require
    [clojure.java.io :as io]
    [clojure.walk :refer [stringify-keys]]
    [io.github.tomboyo.lily.compiler.cg.helpers
     :as helpers
     :refer [map->PackageDecl map->Record map->Method map->Type map->Field]]
    [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast
             AstTemplate Fqn OperationParameter ParameterEncoding
             ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)
           (org.stringtemplate.v4 STGroupFile)))

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
       :meta   #{:withers}})))

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

(def stgf (STGroupFile. (io/resource "templates/template.stg")))

(defn record [m]
  (-> (.getInstanceOf stgf "record")
      (.add "type" (stringify-keys (:type m)))
      (.add "fields" (stringify-keys (:fields m)))
      (.add "body" (stringify-keys (:body m)))))

(defn emptyFactory
  ([m] (emptyFactory m (fn [_] "null")))
  ([m f]
   (-> (.getInstanceOf stgf "emptyFactory")
       (.add "type" (stringify-keys (:type m)))
       (.add "parameters" (map f (:fields m))))))

(defn wither [record field]
  (-> (.getInstanceOf stgf "wither")
      (.add "returns" (stringify-keys (:type record)))
      (.add "field" (stringify-keys field))
      (.add "ctorParams" (map (fn [f] (if (= f field)
                                        (:name f)
                                        (str "this." (:name f))))
                              (:fields record)))))

(defn method [m]
  (-> (.getInstanceOf stgf "method")
      (.add "modifiers" (map name (:modifiers m)))
      (.add "returns" (stringify-keys (:returns m)))
      (.add "name" (:name m))))

(defn render2 [^AstTemplate astTemplate]
  (let [pathParameters (when (.pathParameters astTemplate)
                         (let [it {:type   {:name "PathParameters"}
                                   :fields (map ast/asField (.pathParameters astTemplate))}]
                           (assoc it :body (emptyFactory it))))
        template {:type   (ast/asType astTemplate)
                  :fields (when pathParameters
                            [{:type (:type pathParameters)
                              :name "pathParameters"}])
                  }]
    (.render (record (assoc template :body [(emptyFactory template #(case (-> % :type :name)
                                                                      "PathParameters" "PathParameters.empty()"))
                                            (record pathParameters)])))))

(comment
  (import [io.github.tomboyo.lily.compiler.ast SimpleName ParameterLocation ParameterEncoding])
  (.contents (render (AstTemplate.
                       (.build (Fqn/newBuilder "com.example" "myOperation"))
                       [(OperationParameter. (SimpleName/of "id")
                                             "id"
                                             ParameterLocation/PATH
                                             (ParameterEncoding/simple)
                                             (.build (Fqn/newBuilder "java.lang" "String")))])))

  (render2 (AstTemplate.
             (.build (Fqn/newBuilder "com.example" "myOperation"))
             [(OperationParameter. (SimpleName/of "id")
                                   "id"
                                   ParameterLocation/PATH
                                   (ParameterEncoding/simple)
                                   (.build (Fqn/newBuilder "java.lang" "String")))]))
  )