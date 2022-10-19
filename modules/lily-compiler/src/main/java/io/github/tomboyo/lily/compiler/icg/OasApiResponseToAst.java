package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Ast;
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
   * @param responseCode The response code associated with this ApiResponse
   * @param response The ApiResponse to evaluate
   * @return A stream of AST.
   */
  public static Stream<Ast> evaluateApiResponse(
      PackageName basePackage, String responseCode, ApiResponse response) {
    return Optional.ofNullable(response.getContent())
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(
            schema ->
                OasSchemaToAst.evaluate(
                    basePackage,
                    // for example: Response200, response404
                    SimpleName.of("Response" + responseCode),
                    schema))
        .stream()
        .flatMap(Pair::right);
  }
}
