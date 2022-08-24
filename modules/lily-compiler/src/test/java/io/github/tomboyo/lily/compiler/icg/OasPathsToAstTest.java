package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.icg.OasPathsToAst.EvaluatePathItemResult;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OasPathsToAstTest {

  @Nested
  class Parameters {
    @Test
    void areEvaluatedToAst() {
      try (var mock = mockStatic(OasSchemaToAst.class)) {
        mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
            .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

        OasPathsToAst.evaluatePathItem(
                "p",
                new PathItem()
                    .addParametersItem(
                        new Parameter().name("a").in("path").schema(new BooleanSchema()))
                    .addParametersItem(
                        new Parameter().name("b").in("path").schema(new StringSchema()))
                    .get(
                        new Operation()
                            .operationId("Get")
                            .addParametersItem(
                                new Parameter().name("c").in("query").schema(new IntegerSchema()))
                            .addParametersItem(
                                new Parameter()
                                    .name("d")
                                    .in("header")
                                    .schema(new BooleanSchema()))))
            .forEach(
                x -> {
                  ;
                }); // consume the stream.

        // Schema are generated for all parameters according to OasSchemaToAst.
        mock.verify(
            () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("A"), eq(new BooleanSchema())));
        mock.verify(
            () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("B"), eq(new StringSchema())));
        mock.verify(
            () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("C"), eq(new IntegerSchema())));
        mock.verify(
            () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("D"), eq(new BooleanSchema())));
      }
    }

    @Test
    void operationParametersOverridePathItemParameters() {
      try (var mock = mockStatic(OasSchemaToAst.class)) {
        mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
            .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

        OasPathsToAst.evaluatePathItem(
                "p",
                new PathItem()
                    .addParametersItem(
                        new Parameter().name("a").in("query").schema(new BooleanSchema()))
                    .get(
                        new Operation()
                            .operationId("Get")
                            .addParametersItem(
                                new Parameter().name("a").in("query").schema(new IntegerSchema()))))
            .forEach(
                x -> {
                  ; // consume the stream
                });

        mock.verify(
            () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("A"), eq(new IntegerSchema())));
      }
    }
  }

  @Nested
  class Operations {
    @Test
    void whenOperationHasTags() {
      var actual =
          OasPathsToAst.evaluatePathItem(
                  "p",
                  new PathItem()
                      .get(new Operation().operationId("Get").tags(List.of("tagA", "tagB"))))
              .collect(Collectors.toSet());

      assertThat(
          "All tags should be listed in the result",
          actual,
          is(
              Set.of(
                  new EvaluatePathItemResult(
                      Set.of("tagA", "tagB"),
                      new AstOperation(
                          "Get", new AstReference("p", "GetOperation", List.of(), false)),
                      List.of()))));
    }

    @Test
    void whenOperationHasNoTags() {
      var actual =
          OasPathsToAst.evaluatePathItem(
                  "p", new PathItem().get(new Operation().operationId("Get")))
              .collect(Collectors.toSet());

      assertThat(
          "The default 'other' tag should be listed in the result",
          actual,
          is(
              Set.of(
                  new EvaluatePathItemResult(
                      Set.of("other"),
                      new AstOperation(
                          "Get", new AstReference("p", "GetOperation", List.of(), false)),
                      List.of()))));
    }
  }
}
