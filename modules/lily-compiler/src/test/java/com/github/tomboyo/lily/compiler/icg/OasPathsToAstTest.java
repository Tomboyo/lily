package com.github.tomboyo.lily.compiler.icg;

public class OasPathsToAstTest {
  //  @Nested
  //  public class Tags {
  //    @Test
  //    public void oneOrMoreTags() {
  //      var ast =
  //          evaluate(
  //                  "com.example",
  //                  Map.of(
  //                      "/foo/bar/",
  //                      new PathItem()
  //                          .get(
  //                              new Operation()
  //                                  .operationId("getFooBar")
  //                                  .tags(List.of("tagA", "tagB")))))
  //              .collect(toSet());
  //
  //      var expectedOperation =
  //          new AstOperation(Set.of("tagA", "tagB"), "getFooBar", GET, "/foo/bar/");
  //      assertTrue(
  //          ast.containsAll(
  //              Set.of(
  //                  new AstTaggedOperations("com.example", "Operations",
  // Set.of(expectedOperation)),
  //                  new AstOperationsClassAlias(
  //                      "com.example",
  //                      "tagA",
  //                      new AstReference("com.example", "Operations"),
  //                      Set.of(expectedOperation)),
  //                  new AstOperationsClassAlias(
  //                      "com.example",
  //                      "tagB",
  //                      new AstReference("com.example", "Operations"),
  //                      Set.of(expectedOperation)))),
  //          "An operation with tags is aliased once for each tag.");
  //    }
  //
  //    @Test
  //    public void noTags() {
  //      var ast =
  //          evaluate(
  //                  "com.example",
  //                  Map.of("/foo/bar/", new PathItem().get(new
  // Operation().operationId("getFooBar"))))
  //              .collect(toSet());
  //
  //      var expectedOperation = new AstOperation(Set.of("defaultTag"), "getFooBar", GET,
  // "/foo/bar/");
  //      assertTrue(
  //          ast.containsAll(
  //              Set.of(
  //                  new AstTaggedOperations("com.example", "Operations",
  // Set.of(expectedOperation)),
  //                  new AstOperationsClassAlias(
  //                      "com.example",
  //                      "defaultTag",
  //                      new AstReference("com.example", "Operations"),
  //                      Set.of(expectedOperation)))),
  //          "An operation with no tags is added to the defaultTag tag.");
  //    }
  //
  //    @Test
  //    public void apiEntrypoint() {
  //      var ast =
  //          evaluate(
  //                  "com.example",
  //                  Map.of(
  //                      "/foo/bar",
  //                      new PathItem()
  //                          .get(
  //                              new Operation()
  //                                  .operationId("getFooBar")
  //                                  .tags(List.of("tagA", "tagB")))))
  //              .collect(toSet());
  //
  //      assertTrue(
  //          ast.contains(
  //              new AstApi(
  //                  "com.example",
  //                  "Api",
  //                  Set.of(
  //                      new AstReference("com.example", "TagA"),
  //                      new AstReference("com.example", "TagB")))),
  //          "All tags are added to the main Api entrypoint AST");
  //    }
  //  }
  //
  //  @Test
  //  public void methods() {
  //    var ast =
  //        evaluate(
  //                "com.example",
  //                Map.of(
  //                    "/foo/bar/",
  //                    new PathItem()
  //                        .delete(new Operation().operationId("deleteFooBar"))
  //                        .get(new Operation().operationId("getFooBar"))
  //                        .head(new Operation().operationId("headFooBar"))
  //                        .options(new Operation().operationId("optionsFooBar"))
  //                        .patch(new Operation().operationId("patchFooBar"))
  //                        .post(new Operation().operationId("postFooBar"))
  //                        .put(new Operation().operationId("putFooBar"))
  //                        .trace(new Operation().operationId("traceFooBar"))))
  //            .collect(toSet());
  //
  //    var expectedOperations =
  //        Set.of(
  //            new AstOperation(Set.of("defaultTag"), "deleteFooBar", DELETE, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "getFooBar", GET, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "headFooBar", HEAD, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "optionsFooBar", OPTIONS, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "patchFooBar", PATCH, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "postFooBar", POST, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "putFooBar", PUT, "/foo/bar/"),
  //            new AstOperation(Set.of("defaultTag"), "traceFooBar", TRACE, "/foo/bar/"));
  //    assertTrue(
  //        ast.containsAll(
  //            Set.of(
  //                new AstTaggedOperations("com.example", "Operations", expectedOperations),
  //                new AstOperationsClassAlias(
  //                    "com.example",
  //                    "defaultTag",
  //                    new AstReference("com.example", "Operations"),
  //                    expectedOperations))),
  //        "Every operation method is evaluated to an AstOperation");
  //  }
}
