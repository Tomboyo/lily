(ns io.github.tomboyo.lily.compiler.operations-test
  (:require [clojure.test :refer :all]
            [io.github.tomboyo.lily.compiler.test :refer [fixture generate resolve-name]]))

(use-fixtures :each fixture)

(defn spec [path-template parameters]
  {"paths"
   {path-template
    {"get"
     {"operationId" "getFoo"
      "parameters"  parameters}}}})

(deftest no-path-parameters
  (testing "when there are no path parameters"
    (generate (spec "/foo" [{"name" "foo" "in" "query" "schema" {"type" "string"}}]))
    (is (= "https://example.com/foo"
           (eval `(.. (~(resolve-name "Api/newBuilder"))
                      (uri "https://example.com/")
                      build
                      everyOperation
                      getFoo
                      httpRequest
                      uri
                      toString)))
        "the request does not require any to be bound")
    (is (thrown-with-msg? Exception
                          #"No matching method path found taking 1 args"
                          (eval `(.. (~(resolve-name "Api/newBuilder"))
                                     (uri "https://example.com/")
                                     build
                                     everyOperation
                                     getFoo
                                     (path identity))))
        "there is no .path method for binding parameters")))

(deftest one-or-more-parameters
  (testing "when there are one or more path parameters"
    (generate (spec "/foo/{a}/{b}{c}" (map #(hash-map "name" %
                                                      "in" "path"
                                                      "schema" {"type" "string"})
                                           ["a" "b" "c"])))
    (is (= "https://example.com/foo/a/bc"
           (eval `(.. (~(resolve-name "Api/newBuilder"))
                      (uri "https://example.com/")
                      build
                      everyOperation
                      getFoo
                      (path #(.. % (a "a") (b "b") (c "c")))
                      httpRequest
                      uri
                      toString)))
        "Operations expose a .path method for binding path parameters via anonymous builder")))


