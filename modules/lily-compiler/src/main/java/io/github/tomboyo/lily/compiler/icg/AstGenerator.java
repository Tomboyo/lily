package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstGenerator {

  private final String basePackage;

  private AstGenerator(String basePackage) {
    this.basePackage = basePackage;
  }

  public static Stream<Ast> evaluate(String basePackage, OpenAPI openAPI) {
    return new AstGenerator(basePackage).evaluate(openAPI);
  }

  private Stream<Ast> evaluate(OpenAPI openAPI) {
    return Stream.of(evaluateComponents(openAPI), evaluatePaths(openAPI)).flatMap(identity());
  }

  private Stream<Ast> evaluateComponents(OpenAPI openAPI) {
    return Optional.ofNullable(openAPI.getComponents())
        .map(Components::getSchemas)
        .orElseGet(Map::of)
        .entrySet()
        .stream()
        .flatMap(
            entry -> OasComponentsToAst.evaluate(basePackage, entry.getKey(), entry.getValue()));
  }

  private Stream<Ast> evaluatePaths(OpenAPI openAPI) {
    var evaluatedPathItems =
        Optional.ofNullable(openAPI.getPaths())
            .map(LinkedHashMap::entrySet)
            .orElse(Map.<String, PathItem>of().entrySet())
            .stream()
            .flatMap(
                entry -> {
                  var relativePath = entry.getKey();
                  var pathItem = entry.getValue();
                  return OasPathsToAst.evaluatePathItem(basePackage, relativePath, pathItem);
                })
            .collect(Collectors.toSet());
    var taggedOperations =
        OasPathsToAst.evaluateTaggedOperations(basePackage, evaluatedPathItems)
            .collect(Collectors.toSet());
    var api = OasPathsToAst.evaluateApi(basePackage, taggedOperations);

    return Stream.of(
            evaluatedPathItems.stream()
                .flatMap(result -> result.ast().stream()), // AST for parameter schemas,
            evaluatedPathItems.stream()
                .map(result -> result.operation()), // Ast for operation builders,
            taggedOperations.stream(), // ast for tag groups,
            Stream.of(api)) // and ast for the API root.
        .flatMap(identity());
  }
}
