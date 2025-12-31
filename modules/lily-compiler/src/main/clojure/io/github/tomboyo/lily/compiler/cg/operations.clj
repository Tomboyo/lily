(ns io.github.tomboyo.lily.compiler.cg.operations
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (io.github.tomboyo.lily.compiler.ast AstOperation)
           (io.github.tomboyo.lily.compiler.cg Mustache Source)
           (io.github.tomboyo.lily.compiler.ast Fqn OperationParameter ParameterEncoding ParameterEncoding$Style ParameterLocation SimpleName)
           (java.util Optional)))

(def template (slurp (io/resource "templates/operations.java.mustache")))
(def encoders "io.github.tomboyo.lily.http.encoding.Encoders")

(defn- queryTemplate [parameters]
  (transduce
    (comp (filter #(= ParameterLocation/QUERY (.location %)))
          (map #(str "{" (.apiName %) "}")))
    str
    parameters))

(defn- encoder [parameter encoding]
  (cond
    (and (= ParameterEncoding$Style/FORM (.style encoding)) (.explode encoding))
    ; Note: cannot use (case) on java enums directly.
    (cond
      (or (= ParameterLocation/PATH (.location parameter))
          (= ParameterLocation/QUERY (.location parameter)))
      "smartFormEncoder"

      (= ParameterLocation/HEADER (.location parameter))
      ".formExploded()")

    (and (= ParameterEncoding$Style/SIMPLE (.style encoding)) (not (.explode encoding)))
    (str encoders ".simple()")

    :else
    (throw (RuntimeException. (str "Unsupported encoding: " encoding)))))

(defn- parameter-map [parameter]
  {"fqpt"    (.toFqpString (.typeName parameter))
   "name"    (.lowerCamelCase (.name parameter))
   "apiName" (.apiName parameter)
   "encoder" (encoder parameter (.encoding parameter))})

(defn render [^AstOperation operation]
  (let [path-parameters (eduction (comp (filter #(= ParameterLocation/PATH (.location %)))
                                        (map #(parameter-map %)))
                                  (.parameters operation))]
    (Source. (.name operation)
             (Mustache/writeString
               template
               (str ::render)
               {
                "packageName"           (.packageName (.name operation))
                "className"             (.typeName (.name operation))
                "pathTemplate"          (str/replace (.relativePath operation) #"^/" "")
                "queryTemplate"         (queryTemplate (.parameters operation))
                "method"                (.method operation)
                "pathSmartFormEncoder"  (some #(and (= ParameterLocation/PATH (.location %))
                                                    (= ParameterEncoding$Style/FORM (.style (.encoding %))))
                                              (.parameters operation))
                "querySmartFormEncoder" (some #(and (= ParameterLocation/QUERY (.location %))
                                                    (= ParameterEncoding$Style/FORM (.style (.encoding %))))
                                              (.parameters operation))
                "responseTypeName"      (.toFqpString (.responseName operation))
                "bodyFqpt"              (-> (.requestBody operation) (.map #(.toFqpString %)) (.orElse nil))
                "pathParameters"        path-parameters
                "hasPathParameters"     (not (empty? path-parameters))
                "queryParameters"       (eduction (comp (filter #(= ParameterLocation/QUERY (.location %)))
                                                        (map #(parameter-map %)))
                                                  (.parameters operation))
                "headers"               (eduction (comp (filter #(= ParameterLocation/HEADER (.location %)))
                                                        (map #(parameter-map %)))
                                                  (.parameters operation))
                }))))

(comment
  (macroexpand '(.. (Optional/of "cats") (map str/upper-case) (orElse nil)))
  (macroexpand '(. operation (requestBody) (map #(.toFqpString %)) (orElse nil)))
  )