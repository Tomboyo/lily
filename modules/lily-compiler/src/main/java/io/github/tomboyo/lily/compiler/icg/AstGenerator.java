package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNullElse;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Map;
import java.util.stream.Stream;

public class AstGenerator {
  public static Stream<Ast> evaluate(String basePackage, OpenAPI openAPI) {
    return Stream.concat(
        OasComponentsToAst.evaluate(
            basePackage, requireNonNullElse(openAPI.getComponents(), defaultComponents())),
        OasPathsToAst.evaluate(basePackage, requireNonNullElse(openAPI.getPaths(), Map.of())));
  }

  private static Components defaultComponents() {
    return new Components().schemas(Map.of());
  }
}
