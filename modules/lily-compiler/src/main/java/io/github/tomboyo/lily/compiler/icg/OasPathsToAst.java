package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;
import static io.github.tomboyo.lily.compiler.icg.Support.joinPackages;
import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstApi;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class OasPathsToAst {

  private final String basePackage;

  private OasPathsToAst(String basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Given a tagged collection of operations from {@link #evaluateTaggedOperations(String,
   * Collection)}, return an AstApi over those operations.
   */
  public static AstApi evaluateApi(String basePackage, Set<AstTaggedOperations> taggedOperations) {
    return new AstApi(basePackage, "Api", taggedOperations);
  }

  /**
   * Given the AST from {@link #evaluatePathItem(String, PathItem)} on one or more PathItems, return
   * a Stream describing AstTaggedOperations which group evaluated operations by their OAS tags.
   */
  public static Stream<AstTaggedOperations> evaluateTaggedOperations(
      String basePackage, Collection<EvaluatePathItemResult> results) {
    return new OasPathsToAst(basePackage).evaluateTaggedOperations(results);
  }

  private Stream<AstTaggedOperations> evaluateTaggedOperations(
      Collection<EvaluatePathItemResult> results) {
    return results.stream()
        .flatMap(result -> result.tags().stream().map(tag -> new Pair<>(tag, result.operation())))
        .collect(groupingBy(Pair::left, mapping(Pair::right, toSet())))
        .entrySet()
        .stream()
        .map(
            entry ->
                new AstTaggedOperations(
                    basePackage, entry.getKey() + "Operations", entry.getValue()));
  }

  /** Evaluate a single PathItem (and its operations, nested schemas, etc) to AST. */
  public static Stream<EvaluatePathItemResult> evaluatePathItem(
      String basePackage, PathItem pathItem) {
    return new OasPathsToAst(basePackage).evaluatePathItem(pathItem);
  }

  private Stream<EvaluatePathItemResult> evaluatePathItem(PathItem pathItem) {
    var inheritedParameters = requireNonNullElse(pathItem.getParameters(), List.<Parameter>of());
    return pathItem.readOperationsMap().entrySet().stream()
        .map(
            entry -> {
              var operation = entry.getValue();
              return evaluateOperation(operation, inheritedParameters);
            });
  }

  private EvaluatePathItemResult evaluateOperation(
      Operation operation, List<Parameter> inheritedParameters) {
    var operationName = operation.getOperationId() + "Operation";
    var subordinatePackageName = joinPackages(basePackage, operationName);
    var ownParameters = requireNonNullElse(operation.getParameters(), List.<Parameter>of());
    var ast =
        mergeParameters(inheritedParameters, ownParameters).stream()
            .flatMap(parameter -> evaluateParameter(subordinatePackageName, parameter));

    return new EvaluatePathItemResult(
        getOperationTags(operation),
        new AstOperation(
            operation.getOperationId(),
            new AstReference(basePackage, operationName, List.of(), false)),
        ast);
  }

  private static Set<String> getOperationTags(Operation operation) {
    if (operation.getTags() != null) {
      return new HashSet<>(operation.getTags());
    } else {
      return Set.of("other");
    }
  }

  /** Merge owned parameters with inherited parameters. Owned parameters take precedence. */
  private static Collection<Parameter> mergeParameters(
      List<Parameter> inherited, List<Parameter> owned) {
    return Stream.concat(inherited.stream(), owned.stream())
        .collect(
            toMap(
                param -> new ParameterId(param.getName(), param.getIn()), identity(), (a, b) -> a))
        .values();
  }

  private Stream<Ast> evaluateParameter(String packageName, Parameter parameter) {
    var parameterRefAndAst =
        OasSchemaToAst.evaluate(
            packageName, capitalCamelCase(parameter.getName()), parameter.getSchema());

    return parameterRefAndAst.right();
  }

  public static record EvaluatePathItemResult(
      Set<String> tags, AstOperation operation, Stream<Ast> ast) {}

  private static record ParameterId(String name, String in) {}
}
