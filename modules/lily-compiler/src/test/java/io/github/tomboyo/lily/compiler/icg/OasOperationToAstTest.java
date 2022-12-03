package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.AstSupport.astReferencePlaceholder;
import static io.github.tomboyo.lily.compiler.ast.AstParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.AstParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.ast.AstReference.ref;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astString;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.AstEncoding;
import io.github.tomboyo.lily.compiler.ast.AstParameter;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
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
import io.swagger.v3.oas.models.responses.ApiResponse;
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
            .thenAnswer(
                invocation -> new Pair<>(astBoolean(), Stream.of(astReferencePlaceholder())));

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

        assertThat(
            "Parameter AST is returned", actual.ast(), is(Set.of(astReferencePlaceholder())));

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
            .thenAnswer(
                invocation ->
                    new Pair<>(
                        new AstResponseSum(
                            Fqn.of("com.example.getfoooperation", "GetFooResponse"), Set.of()),
                        Stream.of()));

        OasOperationToAst.evaluateOperaton(
            PackageName.of("com.example"),
            "/foo",
            GET,
            new Operation()
                .operationId("getFoo")
                .responses(
                    new ApiResponses()
                        .addApiResponse("200", new ApiResponse())
                        .addApiResponse("404", new ApiResponse())),
            List.of());

        mock.verify(
            () ->
                OasApiResponsesToAst.evaluateApiResponses(
                    eq(PackageName.of("com.example.getfoooperation")),
                    eq(SimpleName.of("GetFoo")),
                    eq(
                        new ApiResponses()
                            .addApiResponse("200", new ApiResponse())
                            .addApiResponse("404", new ApiResponse()))));
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
            actual().operation().operationClass(),
            is(ref(Fqn.of("p", "OperationIdOperation"), List.of())));
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
                      new AstParameter(
                          SimpleName.of("A"), "a", PATH, AstEncoding.simple(), astBoolean()),
                      Stream.of()))
              .thenReturn(
                  new ParameterAndAst(
                      new AstParameter(
                          SimpleName.of("B"), "b", QUERY, AstEncoding.formExplode(), astString()),
                      Stream.of()));

          assertThat(
              "The parameters list is in the original OAS order",
              actual().operation().parameters(),
              is(
                  List.of(
                      new AstParameter(
                          SimpleName.of("A"), "a", PATH, AstEncoding.simple(), astBoolean()),
                      new AstParameter(
                          SimpleName.of("B"),
                          "b",
                          QUERY,
                          AstEncoding.formExplode(),
                          astString()))));
        }
      }
    }
  }
}
