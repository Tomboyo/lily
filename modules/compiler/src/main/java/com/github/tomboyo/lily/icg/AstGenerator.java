package com.github.tomboyo.lily.icg;

import com.github.tomboyo.lily.ast.Ast;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.stream.Stream;

public class AstGenerator {
  public static Stream<Ast> evaluate(String basePackage, OpenAPI openAPI) {
    return Stream.concat(
        OasSchemaToAst.evaluate(basePackage, openAPI.getComponents().getSchemas()),
        OasPathsToAst.evaluate(basePackage, openAPI.getPaths()));
  }
}
