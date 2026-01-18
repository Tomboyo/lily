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
           (org.stringtemplate.v4 STGroupFile StringRenderer)))

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

(def stgf (let [tmp (STGroupFile. (io/resource "templates/template.stg"))]
            (.registerRenderer tmp String (StringRenderer.))
            tmp))

(defn record [m]
  (-> (.getInstanceOf stgf "record")
      (.add "type" (stringify-keys (:type m)))
      (.add "fields" (stringify-keys (:fields m)))
      (.add "body" (stringify-keys (:body m)))))

(defn wither [{:keys [returns name param ctorParams]}]
  (-> (.getInstanceOf stgf "wither")
      (.add "returns" (stringify-keys returns))
      (.add "name" name)
      (.add "param" (stringify-keys param))
      (.add "ctorParams" ctorParams)))

(defn emptyFactory
  ([m] (emptyFactory m (fn [_] "null")))
  ([m f]
   (-> (.getInstanceOf stgf "emptyFactory")
       (.add "type" (stringify-keys (:type m)))
       (.add "parameters" (map f (:fields m))))))

(defn with-body [m f]
  (update m :body #(conj % (f m))))

(defn withers [{fields :fields :as m}]
  (letfn [(wither-for-field
            [field {:keys [type fields]}]
            (wither {:returns    type
                     :name       (:name field)
                     :param      field
                     :ctorParams (map #(if (= field %)
                                         (:name field)
                                         (str "this." (:name field)))
                                      fields)}))]
    (reduce
      (fn [result field]
        (with-body result #(wither-for-field field %)))
      m
      fields)))

(defn f-withers [{fields :fields :as m}]
  (letfn [(wither-for-field
            [field {:keys [type fields]}]
            (wither {:returns    type
                     :name       (:name field)
                     :param      {:type {:package    "java.util.function"
                                         :name       "Function"
                                         :parameters (repeat 2 (:type field))}
                                  :name "f"}
                     :ctorParams (map #(if (= field %)
                                         (str "f.apply(this." (:name field) ")")
                                         (str "this." (:name field)))
                                      fields)}))]
    (reduce
      (fn [result field]
        (with-body result #(wither-for-field field %)))
      m
      fields)))

(defn render2 [^AstTemplate astTemplate]
  (let [pathParameters (-> {:type   {:name "PathParameters"}
                            :fields (map ast/asField (.pathParameters astTemplate))}
                           (with-body emptyFactory)
                           (withers))
        template (-> {:type   (ast/asType astTemplate)
                      :fields (when pathParameters
                                [{:type (:type pathParameters)
                                  :name "pathParameters"}])
                      :body   [(record pathParameters)]}
                     (with-body #(emptyFactory % (fn [f] (case (-> f :type :name)
                                                           "PathParameters" "PathParameters.empty()"))))
                     (f-withers)
                     (record))]
    (.render template)))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (render2 astTemplate)))

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
                                   (.build (Fqn/newBuilder "java.lang" "String")))
              (OperationParameter. (SimpleName/of "include")
                                   "include"
                                   ParameterLocation/PATH
                                   (ParameterEncoding/simple)
                                   (.build (Fqn/newBuilder "java.lang" "String")))]))
  )