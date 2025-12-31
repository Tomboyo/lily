(ns io.github.tomboyo.lily.compiler.test
  (:import (io.github.tomboyo.lily.compiler CompilerSupport)))

(def package-name
  "The package into which sources are generated."
  "gen")

; Bound by fixture.
(def ^:dynamic *state*)

(defn generate
  "Generates source code from the given OpenAPI specification. Sources are
  placed in the 'gen' package."
  ([spec]
   (dosync (ref-set *state* (generate @*state* spec))))

  ([state spec]
   (when (:dirty state)
     (throw (IllegalStateException. "Tried to generate source code twice in one package")))

   (^[String String] CompilerSupport/compileOas package-name spec)
   (assoc state :dirty true)))

;; Test fixtures

(defn fixture
  "This fixture configures test/generate to generate sources into a unique
  package that will be cleaned up after the test or tests complete."
  [f]
  (binding [*state* (ref {})]
    (try
      (f)
      (finally (CompilerSupport/clearPackageFiles package-name)))))

(comment
  ; Generate the spec and get the class associated with the generated getFoo operation, then clean up.
  (fixture (fn []
             (generate
               "paths:
                  /foo/{id}:
                    get:
                      operationId: getFoo
                      parameters:
                        - name: id
                          in: path
                          schema:
                            type: string
               ")
             (import gen.Api)
             ; Will typically need to prevent compilation using eval and a quote.
             (eval '(.. (Api/newBuilder)
                       (uri "https://example.com")
                       build
                       everyOperation
                       getFoo
                       )))))
