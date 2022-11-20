package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Optional;
import java.util.stream.Stream;

public class OasApiResponseToAst {

  /**
   * Generate AST for application/json response schema, if any.
   *
   * @param basePackage The root package name under which to generate types
   * @param operationId The ID of the operation which references this response schema ("GetFoo", not
   *     "GetFooOperation")
   * @param responseCode The response code associated with this ApiResponse
   * @param response The ApiResponse to evaluate
   * @return A stream tuples, one each for response, whose left value is a reference to a generated
   *     response type, and whose right value is a stream of subordinate AST.
   */
  public static Stream<Pair<AstReference, Stream<Ast>>> evaluateApiResponse(
      PackageName basePackage, SimpleName operationId, String responseCode, ApiResponse response) {
    return Optional.ofNullable(response.getContent())
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(
            schema ->
                OasSchemaToAst.evaluate(
                    basePackage,
                    // The name for an "anonymous" response schema (i.e. not a component)
                    // for example: GetPetById200, GetPetById404
                    operationId.resolve(responseCode),
                    schema))
        .stream();
  }
}
