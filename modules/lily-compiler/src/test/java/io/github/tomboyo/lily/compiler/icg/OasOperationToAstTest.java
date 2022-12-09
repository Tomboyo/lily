package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.AstSupport.astPlaceholder;
import static io.github.tomboyo.lily.compiler.AstSupport.fqnPlaceholder;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astString;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.OperationParameter;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.icg.OasOperationToAst.TagsOperationAndAst;
import io.github.tomboyo.lily.compiler.icg.OasParameterToAst.ParameterAndAst;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OasOperationToAstTest {
  @Nested
  class EvaluateOperation {
    @Test
    void evaluatesAllParametersToAst() {
      try (var mock = mockStatic(OasSchemaToAst.class)) {
        mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
            .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of(fqnPlaceholder())));

        var actual =
            OasOperationToAst.evaluateOperaton(
                PackageName.of("p"),
                "/relative/path",
                GET,
                new Operation()
                    .operationId("operationId")
                    .addParametersItem(
                        new Parameter().name("a").in("path").schema(new IntegerSchema()))
                    .addParametersItem(
                        new Parameter().name("b").in("query").schema(new DateSchema())),
                List.of(
                    new Parameter().name("c").in("header").schema(new BooleanSchema()),
                    new Parameter().name("d").in("cookie").schema(new StringSchema())));

        assertThat("Parameter AST is returned", actual.ast(), is(Set.of(fqnPlaceholder())));

        // Each parameter is evaluated to AST in turn
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("A")),
                    eq(new IntegerSchema())));
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("B")),
                    eq(new DateSchema())));
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("C")),
                    eq(new BooleanSchema())));
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("D")),
                    eq(new StringSchema())));
      }
    }

    @Test
    void overridesInheritedParameters() {
      try (var mock = mockStatic(OasSchemaToAst.class)) {
        mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
            .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

        OasOperationToAst.evaluateOperaton(
            PackageName.of("p"),
            "/relative/path",
            GET,
            new Operation()
                .operationId("operationId")
                .addParametersItem(
                    new Parameter()
                        .name("a")
                        .in("query")
                        .schema(new IntegerSchema())), // the only IntegerSchema
            List.of(
                new Parameter().name("a").in("path").schema(new BooleanSchema()), // different `in`
                new Parameter()
                    .name("b")
                    .in("query")
                    .schema(new BooleanSchema()), // different `name`
                new Parameter()
                    .name("a")
                    .in("query")
                    .schema(new BooleanSchema()) // equal `name` and `in`
                ));

        // The PathItem's "a" query parameter is overridden by the Operation's query parameter with
        // the same name.
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("A")),
                    eq(new IntegerSchema())));
        // The other parameters are not affected.
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("A")),
                    eq(new BooleanSchema())));
        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("p.operationidoperation")),
                    eq(SimpleName.of("B")),
                    eq(new BooleanSchema())));
      }
    }

    @Test
    void evaluatesApiResponsesToAst() {
      try (var mock = mockStatic(OasApiResponsesToAst.class)) {
        mock.when(() -> OasApiResponsesToAst.evaluateApiResponses(any(), any(), any()))
            .thenAnswer(invocation -> Stream.of(astPlaceholder()));

        var actual =
            OasOperationToAst.evaluateOperaton(
                    PackageName.of("com.example"),
                    "/foo",
                    GET,
                    new Operation().operationId("getFoo").responses(new ApiResponses()),
                    List.of())
                .ast();

        assertThat(
            "Response ast is flattened into the AST stream", actual, hasItems(astPlaceholder()));
      }
    }

    @Nested
    class Tags {
      @Test
      void whenOasOperationHasTags() {
        try (var mock = mockStatic(OasSchemaToAst.class)) {
          mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
              .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

          var actual =
              OasOperationToAst.evaluateOperaton(
                  PackageName.of("p"),
                  "/relative/path/",
                  GET,
                  new Operation().operationId("operationId").tags(List.of("tagA", "tagB")),
                  List.of());

          assertThat(
              "The result contains all OAS tags and the 'all' tag",
              actual.tags(),
              is(Set.of("tagA", "tagB", "all")));
        }
      }

      @Test
      void whenOasOperationHasNoTags() {
        try (var mock = mockStatic(OasSchemaToAst.class)) {
          mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
              .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

          var actual =
              OasOperationToAst.evaluateOperaton(
                  PackageName.of("p"),
                  "/relative/path/",
                  GET,
                  new Operation().operationId("operationId").tags(List.of()), // empty!
                  List.of());

          assertThat(
              "The result contains the default 'other' tag and the 'all' tag",
              actual.tags(),
              is(Set.of("other", "all")));
        }
      }
    }

    @Nested
    class AstOperation {
      TagsOperationAndAst actual() {
        return OasOperationToAst.evaluateOperaton(
            PackageName.of("p"),
            "/relative/path/",
            GET,
            new Operation()
                .operationId("operationId")
                .addParametersItem(new Parameter().name("a").in("path").schema(new IntegerSchema()))
                .addParametersItem(
                    new Parameter().name("b").in("query").schema(new IntegerSchema())),
            List.of());
      }

      @Test
      void containsOasOperationName() {
        assertThat(
            "The operation name is taken from the globally unique OAS operationID",
            actual().operation().operationName(),
            is(SimpleName.of("operationId")));
      }

      @Test
      void referencesNewOperationClass() {
        assertThat(
            "The AstReference points to a generated type named after the operation ID",
            actual().operation().name(),
            is(Fqn.newBuilder().packageName("p").typeName("OperationIdOperation").build()));
      }

      @Test
      void containsRelativePath() {
        assertThat(
            "The operation's relative path is as given",
            actual().operation().relativePath(),
            is("/relative/path/"));
      }

      @Test
      void containsMethod() {
        assertThat(
            "The operation's http method is as given", actual().operation().method(), is("GET"));
      }

      @Test
      void containsParametersList() {
        try (var mock = mockStatic(OasParameterToAst.class)) {
          mock.when(() -> OasParameterToAst.evaluateParameter(any(), any()))
              .thenReturn(
                  new ParameterAndAst(
                      new OperationParameter(
                          SimpleName.of("A"), "a", PATH, ParameterEncoding.simple(), astBoolean()),
                      Stream.of()))
              .thenReturn(
                  new ParameterAndAst(
                      new OperationParameter(
                          SimpleName.of("B"),
                          "b",
                          QUERY,
                          ParameterEncoding.formExplode(),
                          astString()),
                      Stream.of()));

          assertThat(
              "The parameters list is in the original OAS order",
              actual().operation().parameters(),
              is(
                  List.of(
                      new OperationParameter(
                          SimpleName.of("A"), "a", PATH, ParameterEncoding.simple(), astBoolean()),
                      new OperationParameter(
                          SimpleName.of("B"),
                          "b",
                          QUERY,
                          ParameterEncoding.formExplode(),
                          astString()))));
        }
      }
    }
  }
}
