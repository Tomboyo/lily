package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import io.github.tomboyo.lily.compiler.ast.AstApi;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.icg.OasOperationToAst.TagsOperationAndAst;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Collection;
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
      String basePackage, Collection<TagsOperationAndAst> results) {
    return new OasPathsToAst(basePackage).evaluateTaggedOperations(results);
  }

  /** Evaluate a single PathItem (and its operations, nested schemas, etc) to AST. */
  public static Stream<TagsOperationAndAst> evaluatePathItem(
      String basePackage, String relativePath, PathItem pathItem) {
    return new OasPathsToAst(basePackage).evaluatePathItem(relativePath, pathItem);
  }

  private Stream<AstTaggedOperations> evaluateTaggedOperations(
      Collection<TagsOperationAndAst> results) {
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

  private Stream<TagsOperationAndAst> evaluatePathItem(String relativePath, PathItem pathItem) {
    var inheritedParameters = requireNonNullElse(pathItem.getParameters(), List.<Parameter>of());
    return pathItem.readOperationsMap().values().stream()
        .map(
            operation ->
                OasOperationToAst.evaluateOperaton(
                    basePackage, relativePath, operation, inheritedParameters));
  }
}
