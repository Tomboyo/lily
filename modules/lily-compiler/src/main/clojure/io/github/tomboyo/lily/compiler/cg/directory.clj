(ns io.github.tomboyo.lily.compiler.cg.directory
  (:require [io.github.tomboyo.lily.compiler.cg.helpers :as helpers :refer [map->ClassDef map->Method map->PackageDecl]]
            [io.github.tomboyo.lily.compiler.cg.interop.ast :as ast])
  (:import (io.github.tomboyo.lily.compiler.ast AstDirectory)
           (io.github.tomboyo.lily.compiler.cg Source)))

(defn static-factory [template]
  (map->Method {:modifiers [:public :static]
                :returns (ast/asType template)
                :name (.. template name typeName lowerCamelCase)
                :body [(str "return new "
                            (helpers/render (ast/asType template))
                            "();")]}))

(defn render [^AstDirectory directory]
  (Source.
    (.name directory)
    (helpers/render
      (let [type (ast/asType directory)]
        [(map->PackageDecl type)
         (map->ClassDef
           {:type type
            :body (map static-factory (.templates directory))})]))))
