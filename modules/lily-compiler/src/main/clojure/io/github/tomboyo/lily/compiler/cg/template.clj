(ns io.github.tomboyo.lily.compiler.cg.template
  (:require
    [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast]
    [io.github.tomboyo.lily.compiler.cg.string-template :as st])
  (:import (io.github.tomboyo.lily.compiler.ast
             AstTemplate Fqn OperationParameter ParameterEncoding
             ParameterLocation SimpleName)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn with-body [m f]
  (update m :body #(conj % (f m))))

(defn add-empty-fn
  ([m] (add-empty-fn m (fn [_] "null")))
  ([m f] (with-body m #(st/empty-fn (-> (select-keys % #{:type})
                                        (assoc :parameters (map f (:fields m))))))))

(defn add-withers [{fields :fields :as m}]
  (letfn [(wither-for-field
            [field {:keys [type fields]}]
            (st/wither {:returns    type
                        :name       (:name field)
                        :param      field
                        :ctorParams (map #(if (= field %)
                                            (:name %)
                                            (str "this." (:name %)))
                                         fields)}))]
    (reduce
      (fn [result field]
        (with-body result #(wither-for-field field %)))
      m
      fields)))

(defn add-fwithers [{fields :fields :as m}]
  (letfn [(wither-for-field
            [field {:keys [type fields]}]
            (st/wither {:returns    type
                        :name       (:name field)
                        :param      {:type {:package    "java.util.function"
                                            :name       "Function"
                                            :parameters (repeat 2 (:type field))}
                                     :name "f"}
                        :ctorParams (map #(if (= field %)
                                            (str "f.apply(this." (:name %) ")")
                                            (str "this." (:name %)))
                                         fields)}))]
    (reduce
      (fn [result field]
        (with-body result #(wither-for-field field %)))
      m
      fields)))

(defn render [^AstTemplate astTemplate]
  (Source. (.name astTemplate)
           (let [pathParameters (-> {:type   {:name "PathParameters"}
                                     :fields (map ast/asField (.pathParameters astTemplate))}
                                    (add-empty-fn)
                                    (add-withers))
                 template (-> {:type   (ast/asType astTemplate)
                               :fields (when pathParameters
                                         [{:type (:type pathParameters)
                                           :name "pathParameters"}])
                               :body   [(st/record pathParameters)]}
                              (add-empty-fn (fn [f] (case (-> f :type :name)
                                                      "PathParameters" "PathParameters.empty()")))
                              (add-fwithers)
                              (st/record))]
             (.render template))))

(comment
  (import [io.github.tomboyo.lily.compiler.ast SimpleName ParameterLocation ParameterEncoding])
  (.contents (render (AstTemplate.
                       (.build (Fqn/newBuilder "com.example" "myOperation"))
                       [(OperationParameter. (SimpleName/of "id")
                                             "id"
                                             ParameterLocation/PATH
                                             (ParameterEncoding/simple)
                                             (.build (Fqn/newBuilder "java.lang" "String")))])))

  (.contents (render (AstTemplate.
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
                                             (.build (Fqn/newBuilder "java.lang" "String")))])))
  )