(ns io.github.tomboyo.lily.compiler.templates-test
  (:require [clojure.test :refer :all]
            [io.github.tomboyo.lily.compiler.test :refer [fixture generate resolve-name relative-class-name]]))

(use-fixtures :each fixture)

(deftest templates-bind-parameters
  (testing "the user can bind and retrieve path parameters to/from a template"
    (generate {"paths" {"/foo/{id}" {"get" {"operationId" "getFooById"
                                            "parameters"  [{"name"   "id"
                                                            "in"     "path"
                                                            "schema" {"type" "string"}}]}}}})
    (is (= "my-id"
           (eval `(.. (~(resolve-name "Directory/getFooById"))
                      (withPathParameters #(.. % (withId "my-id")))
                      pathParameters
                      id)
                 )))))
