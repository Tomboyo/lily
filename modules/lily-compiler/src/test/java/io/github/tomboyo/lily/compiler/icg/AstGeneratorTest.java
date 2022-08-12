package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.oas.OasReader;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

/** A high-level, non-exhaustive integration test of the AstGenerator API. Here we use low-effort happy-path tests to gain some confidence that the AstGenerator is still integrating with subordinate modules to generate AST. */
public class AstGeneratorTest {

  @Test
  void paths() throws Exception {
    var actual = AstGenerator.evaluate("p", OasReader.fromString(
        """
        paths:
          /foo/{id}/:
            parameters:
              - name: id
                in: path
                schema:
                  type: object
                  parameters:
                    value:
                      type: string
            get:
              operationId: GetFoo
              parameters:
                - name: foo
                  in: query
                  schema:
                    type: boolean
        """,
        true));

    assertThat(
        actual.collect(Collectors.toSet()),
        hasItems(
          AstClass.of("p.getfoo", "Id", List.of(
            new AstField(astString(), "value")))));
  }
}
