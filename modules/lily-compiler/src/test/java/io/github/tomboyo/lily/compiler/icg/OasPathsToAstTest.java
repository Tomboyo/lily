package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
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
}
