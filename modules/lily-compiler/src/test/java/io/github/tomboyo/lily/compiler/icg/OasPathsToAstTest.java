package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.icg.OasPathsToAst.EvaluatedOperation;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OasPathsToAstTest {

  @Nested
  class EvaluatePathItem {

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
              .forEach(x -> {}); // consume the stream.

          // Schema are generated for all parameters according to OasSchemaToAst.
          mock.verify(
              () ->
                  OasSchemaToAst.evaluate(eq("p.getoperation"), eq("A"), eq(new BooleanSchema())));
          mock.verify(
              () -> OasSchemaToAst.evaluate(eq("p.getoperation"), eq("B"), eq(new StringSchema())));
          mock.verify(
              () ->
                  OasSchemaToAst.evaluate(eq("p.getoperation"), eq("C"), eq(new IntegerSchema())));
          mock.verify(
              () ->
                  OasSchemaToAst.evaluate(eq("p.getoperation"), eq("D"), eq(new BooleanSchema())));
        }
      }

      @Test
      void mayBeOverridden() {
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
                                  new Parameter()
                                      .name("a")
                                      .in("query")
                                      .schema(new IntegerSchema()))))
              .forEach(x -> {});

          mock.verify(
              () ->
                  OasSchemaToAst.evaluate(eq("p.getoperation"), eq("A"), eq(new IntegerSchema())));
        }
      }
    }

    @Nested
    class OperationTags {
      EvaluatedOperation actual(String... tags) {
        return OasPathsToAst.evaluatePathItem(
                "p",
                new PathItem().get(new Operation().operationId("Get").tags(Arrays.asList(tags))))
            .findAny()
            .orElseThrow(); // Exactly one expected since there's one OAS operation
      }

      @Test
      void whenOperationHasTags() {
        assertThat(
            "The OAS tags and the default 'all' tag are added to the result",
            actual("tagA", "tagB").tags(),
            is(Set.of("tagA", "tagB", "all")));
      }

      @Test
      void whenOperationHasNoTags() {
        assertThat(
            "The default 'other' and 'all' tags are added to the result",
            actual().tags(),
            is(Set.of("other", "all")));
      }
    }

    @Nested
    class AstOperation {
      EvaluatedOperation actual() {
        return OasPathsToAst.evaluatePathItem(
                "p", new PathItem().get(new Operation().operationId("Get")))
            .findAny()
            .orElseThrow(); // Exactly one expected since there's one OAS operation
      }

      @Test
      void containsOasOperationName() {
        assertThat(actual().operation().operationName(), is("Get"));
      }

      @Test
      void referencesNewOperationClass() {
        assertThat(
            "The result references a new operation named after the operation ID",
            actual().operation().operationClass(),
            is(new AstReference("p", "GetOperation", List.of(), false)));
      }
    }
  }

  @Nested
  class TaggedOperations {
    @Test
    void groupsByTags() {
      var getAOperation =
          new AstOperation("GetA", new AstReference("p", "GetAOperation", List.of(), false));
      var getABOperation =
          new AstOperation("GetAB", new AstReference("p", "GetABOperation", List.of(), false));
      var actual =
          OasPathsToAst.evaluateTaggedOperations(
                  "p",
                  List.of(
                      new EvaluatedOperation(Set.of("TagA"), getAOperation, List.of()),
                      new EvaluatedOperation(Set.of("TagA", "TagB"), getABOperation, List.of())))
              .collect(Collectors.toSet());

      assertThat(
          "Tagged operations AST are collections of operations grouped by tag",
          actual,
          is(
              Set.of(
                  new AstTaggedOperations(
                      "p", "TagAOperations", Set.of(getAOperation, getABOperation)),
                  new AstTaggedOperations("p", "TagBOperations", Set.of(getABOperation)))));
    }
  }
}
