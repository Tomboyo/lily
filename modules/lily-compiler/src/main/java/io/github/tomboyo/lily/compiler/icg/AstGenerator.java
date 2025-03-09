package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.Definition;
import io.github.tomboyo.lily.compiler.ast.Modifier;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.oas.model.Components;
import io.github.tomboyo.lily.compiler.oas.model.OpenApi;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstGenerator {

  private final PackageName basePackage;

  private AstGenerator(PackageName basePackage) {
    this.basePackage = basePackage;
  }

  public static Stream<Ast> evaluate(PackageName basePackage, OpenApi openApi) {
    return new AstGenerator(basePackage).evaluate(openApi);
  }

  private Stream<Ast> evaluate(OpenApi openApi) {
    var table =
        Stream.of(evaluateComponents(openApi), evaluatePaths(openApi))
            .flatMap(identity())
            /*
             * Collect definitions into a FQN -> AST table. This gives us an
             * opportunity to modify AST generated from schema encountered early based
             * on schema encountered late. We have to do this to create sealed
             * interfaces, for example, which reference and are referenced by their
             * permitted members.
             */
            .collect(
                Collectors.toMap(
                    ast -> ast.name(),
                    ast -> ast,
                    (left, right) -> {
                      if (left instanceof Modifier mod && right instanceof Definition def) {
                        return mod.modify(def);
                      } else if (left instanceof Definition def && right instanceof Modifier mod) {
                        return mod.modify(def);
                      } else {
                        throw new IllegalStateException("Tried to assign two AST to one FQN");
                      }
                    }));
    return table.values().stream();
  }

  private Stream<Ast> evaluateComponents(OpenApi openApi) {
    return openApi.components().map(Components::schemas).orElse(Map.of()).entrySet().stream()
        .flatMap(
            entry ->
                OasComponentsToAst.evaluate(
                    basePackage, SimpleName.of(entry.getKey()), entry.getValue()));
  }

  private Stream<Ast> evaluatePaths(OpenApi openApi) {
    var evaluatedPathItems =
        openApi.paths().entrySet().stream()
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
