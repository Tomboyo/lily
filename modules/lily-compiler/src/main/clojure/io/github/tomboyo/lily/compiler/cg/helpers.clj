(ns io.github.tomboyo.lily.compiler.cg.helpers
  (:require [clojure.string :as str])
  (:import (clojure.lang Keyword Seqable)))

(defn indent [s]
  (if (nil? s)
    s
    (str/join "\n" (map #(str "  " %) (str/split-lines s)))))

(defn lowerCamelCase
  "returns name in lowerCamelCase"
  [name]
  (str
    (str/lower-case (nth name 0))
    (subs name 1)))

(defn upperCamelCase
  "returns name in upperCamelCase"
  [name]
  (str
    (str/upper-case (nth name 0))
    (subs name 1)))

(defprotocol Render
  (render [x]))

(extend-protocol Render
  nil
  (render [_] nil)

  String
  (render [s] s)

  Keyword
  (render [k] (subs (str k) 1))

  Seqable
  (render [xs] (str/join "\n" (eduction (comp (filter (complement nil?))
                                              (map render))
                                        xs)))
  )

(defrecord PackageDecl [package]
  Render
  (render [_]
    (str "package " package ";")))

(defrecord Field [type name]
  Render
  (render [_]
    (str (render type) " " name)))

(defn toField
  "Converts anything with a :type to a field `NameOfType nameOfType`."
  [x]
  (->Field (:type x) (lowerCamelCase (-> x :type :name))))

(defrecord Method [modifiers returns name args body]
  Render
  (render [_]
    (render [(str (str/join " " (map render modifiers))
                  " "
                  (render returns)
                  " "
                  name
                  "(" (str/join ", " (map render args)) ") {")
             (-> body render indent)
             "}"])))

(defrecord Type [package name parameters]
  Render
  (render [_] (let [pn (str/join "." (filter (complement nil?) [package name]))]
                (if (not-empty parameters)
                  (str pn "<" (str/join ", " (map render parameters)) ">")
                  pn))))

(defn renderNew
  "Render a constructor call `new MyFoo(...)` for the given type. Each field of
  fields is transformed into a parameter string by xform."
  [type fields xform]
  (str "new " (render type) "(" (str/join ", " (map xform fields)) ")"))

(defn fwither [record field]
  (map->Method
    {:modifiers [:public]
     :returns   (:type record)
     :name      (str "with" (-> field :name render upperCamelCase))
     :args      [(map->Field {:type (map->Type {:package    "java.util.function"
                                                :name       "Function"
                                                :parameters (repeat 2 (:type field))})
                              :name "f"})]
     :body      (str "return "
                     (renderNew (:type record) (:fields record) #(if (= % field)
                                                                   (str "f.apply(this." (:name %) ")")
                                                                   (str "this." (:name %))))
                     ";")
     }))

(defn wither [{type :type fields :fields} field]
  (map->Method
    {:modifiers [:public]
     :returns type
     :name (str "with" (-> field :name render upperCamelCase))
     :args [field]
     :body (str "return " (renderNew type fields #(if (= % field)
                                                    (:name %)
                                                    (str "this." (:name %))))
                ";")}))

(defrecord Record [type fields body meta]
  Render
  (render [r]
    (let [header (str/join ", " (map render fields))
          fwithers (when (:fwithers meta) (map #(fwither r %) fields))
          withers (when (:withers meta) (map #(wither r %) fields))]
      (render [(str "public record " (:name type) "(" header ") {")
               (-> body render indent)
               (-> fwithers render indent)
               (-> withers render indent)
               "}"]))))


(defrecord ClassDef [type body]
  Render
  (render [_]
    (render [(str "public class " (:name type) " {")
             (-> body render indent)
             "}"])))

(def emptyFactoryName
  "The name of static factories for 'empty' instances of things."
  "empty")

(defn staticFactory
  [type fields xform]
  (map->Method {:modifiers [:public :static]
                :returns   type
                :name      emptyFactoryName
                :body      [(str "return " (renderNew type fields xform) ";")]
                }))

(comment
  (render (map->Record {:type {:package "com.example.template"
                               :name    "MyRecord"}}))
  (render (let [return-type (map->Type {:package "com.example.template"
                                        :name    "GetFooTemplate"})
                method (map->Method {:modifiers   [:public :static]
                                     :return-type return-type
                                     :name        "getFoo"
                                     :body        [(str "return new " (render return-type) "();")]
                                     })
                class-def (map->ClassDef {:type {:package "com.example" :name "Directory"}
                                          :body [method]})]
            class-def)))