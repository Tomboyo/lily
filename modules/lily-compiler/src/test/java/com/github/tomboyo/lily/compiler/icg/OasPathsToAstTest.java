package com.github.tomboyo.lily.compiler.icg;

import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.DELETE;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.GET;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.HEAD;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.OPTIONS;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.PATCH;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.POST;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.PUT;
import static com.github.tomboyo.lily.compiler.ast.AstOperation.Method.TRACE;
import static com.github.tomboyo.lily.compiler.icg.OasPathsToAst.evaluate;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomboyo.lily.compiler.ast.AstOperation;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClass;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstReference;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OasPathsToAstTest {
  @Nested
  public class Tags {
    @Test
    public void oneOrMoreTags() {
      var ast =
          evaluate(
                  "com.example",
                  Map.of(
                      "/foo/bar/",
                      new PathItem()
                          .get(
                              new Operation()
                                  .operationId("getFooBar")
                                  .tags(List.of("tagA", "tagB")))))
              .collect(toSet());

      var expectedOperation =
          new AstOperation(Set.of("tagA", "tagB"), "getFooBar", GET, "/foo/bar/");
      assertEquals(
          Set.of(
              new AstOperationsClass("com.example", "Operations", Set.of(expectedOperation)),
              new AstOperationsClassAlias(
                  "com.example",
                  "tagA",
                  new AstReference("com.example", "Operations"),
                  Set.of(expectedOperation)),
              new AstOperationsClassAlias(
                  "com.example",
                  "tagB",
                  new AstReference("com.example", "Operations"),
                  Set.of(expectedOperation))),
          ast,
          "An operation with tags is aliased once for each tag.");
    }

    @Test
    public void noTags() {
      var ast =
          evaluate(
                  "com.example",
                  Map.of("/foo/bar/", new PathItem().get(new Operation().operationId("getFooBar"))))
              .collect(toSet());

      var expectedOperation = new AstOperation(Set.of("defaultTag"), "getFooBar", GET, "/foo/bar/");
      assertEquals(
          Set.of(
              new AstOperationsClass("com.example", "Operations", Set.of(expectedOperation)),
              new AstOperationsClassAlias(
                  "com.example",
                  "defaultTag",
                  new AstReference("com.example", "Operations"),
                  Set.of(expectedOperation))),
          ast,
          "An operation with no tags is added to the defaultTag tag.");
    }
  }

  @Test
  public void methods() {
    var ast =
        evaluate(
                "com.example",
                Map.of(
                    "/foo/bar/",
                    new PathItem()
                        .delete(new Operation().operationId("deleteFooBar"))
                        .get(new Operation().operationId("getFooBar"))
                        .head(new Operation().operationId("headFooBar"))
                        .options(new Operation().operationId("optionsFooBar"))
                        .patch(new Operation().operationId("patchFooBar"))
                        .post(new Operation().operationId("postFooBar"))
                        .put(new Operation().operationId("putFooBar"))
                        .trace(new Operation().operationId("traceFooBar"))))
            .collect(toSet());

    var expectedOperations =
        Set.of(
            new AstOperation(Set.of("defaultTag"), "deleteFooBar", DELETE, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "getFooBar", GET, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "headFooBar", HEAD, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "optionsFooBar", OPTIONS, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "patchFooBar", PATCH, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "postFooBar", POST, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "putFooBar", PUT, "/foo/bar/"),
            new AstOperation(Set.of("defaultTag"), "traceFooBar", TRACE, "/foo/bar/"));
    assertEquals(
        Set.of(
            new AstOperationsClass("com.example", "Operations", expectedOperations),
            new AstOperationsClassAlias(
                "com.example",
                "defaultTag",
                new AstReference("com.example", "Operations"),
                expectedOperations)),
        ast,
        "Every operation method is evaluated to an AstOperation");
  }
}
