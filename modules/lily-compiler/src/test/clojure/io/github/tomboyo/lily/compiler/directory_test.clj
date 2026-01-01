(ns io.github.tomboyo.lily.compiler.directory-test
  (:require [clojure.test :refer :all]
            [io.github.tomboyo.lily.compiler.test :refer [fixture generate resolve-name relative-class-name]]))

(use-fixtures :each fixture)

(deftest directory-aggregates-templates
  (testing "the Directory exposes a static function to create a Template for each operation in the specification"
    (generate {"paths" {"/foo"          {"get"  {"operationId" "getFoo"}
                                         "post" {"operationId" "createFoo"}}
                        "/bar/baz/bang" {"delete" {"operationId" "deleteBang"}}}})
    (are [x y] (= x (relative-class-name (eval `(~(resolve-name y)))))
               "templates.GetFoo" "Directory/getFoo"
               "templates.CreateFoo" "Directory/createFoo"
               "templates.DeleteBang" "Directory/deleteBang")))
