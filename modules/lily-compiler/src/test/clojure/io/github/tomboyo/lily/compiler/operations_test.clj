(ns io.github.tomboyo.lily.compiler.operations-test
  (:require [clojure.test :refer :all]
            [io.github.tomboyo.lily.compiler.test :as test-compile]))

(use-fixtures :each test-compile/fixture)

(deftest parameters
  (testing "binding path parameters"
    (is (= "https://example.com/foo/id-parameter"
           (do
             (test-compile/generate
               "paths:
                 /foo/{id}:
                   get:
                     operationId: getFoo
                     parameters:
                       - name: id
                         in: path
                         schema:
                           type: string")
             (import gen.Api)
             (eval '(.. (Api/newBuilder)
                        (uri "https://example.com/")
                        build
                        everyOperation
                        getFoo
                        (path #(.id % "id-parameter"))
                        httpRequest
                        uri
                        toString)))))
    ))