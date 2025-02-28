package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import io.github.tomboyo.lily.compiler.ast.AstApi;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.icg.OasOperationToAst.TagsOperationAndAst;
import io.github.tomboyo.lily.compiler.oas.model.PathItem;
import io.github.tomboyo.lily.compiler.util.Pair;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public class OasPathsToAst {

  private final PackageName basePackage;

  private OasPathsToAst(PackageName basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Given a tagged collection of operations from {@link #evaluateTaggedOperations(PackageName,
   * Collection)}, return an AstApi over those operations.
   */
  public static AstApi evaluateApi(
      PackageName basePackage, Set<AstTaggedOperations> taggedOperations) {
    return new AstApi(
        Fqn.newBuilder().packageName(basePackage).typeName(SimpleName.of("Api")).build(),
        taggedOperations);
  }

  /**
   * Given the AST from {@link #evaluatePathItem(String, PathItem)} on one or more PathItems, return
   * a Stream describing AstTaggedOperations which group evaluated operations by their OAS tags.
   */
  public static Stream<AstTaggedOperations> evaluateTaggedOperations(
      PackageName basePackage, Collection<TagsOperationAndAst> results) {
    return new OasPathsToAst(basePackage).evaluateTaggedOperations(results);
  }

  /** Evaluate a single PathItem (and its operations, nested schemas, etc) to AST. */
  public static Stream<TagsOperationAndAst> evaluatePathItem(
      PackageName basePackage, String relativePath, PathItem pathItem) {
    return new OasPathsToAst(basePackage).evaluatePathItem(relativePath, pathItem);
  }

  private Stream<AstTaggedOperations> evaluateTaggedOperations(
      Collection<TagsOperationAndAst> results) {
    var everyOperation =
        new AstTaggedOperations(
            Fqn.newBuilder().packageName(basePackage).typeName("EveryOperation").build(),
            results.stream().map(TagsOperationAndAst::operation).collect(toSet()));
    var everyUntaggedOperation =
        new AstTaggedOperations(
            Fqn.newBuilder().packageName(basePackage).typeName("EveryUntaggedOperation").build(),
            results.stream()
                .filter(x -> x.tags().isEmpty())
                .map(TagsOperationAndAst::operation)
                .collect(toSet()));
    var taggedOperations =
        results.stream()
            .flatMap(
                result -> result.tags().stream().map(tag -> new Pair<>(tag, result.operation())))
            .collect(groupingBy(Pair::left, mapping(Pair::right, toSet())))
            .entrySet()
            .stream()
            .map(
                entry ->
                    new AstTaggedOperations(
                        Fqn.newBuilder()
                            .packageName(basePackage)
                            .typeName(SimpleName.of(entry.getKey()).resolve("operations"))
                            .build(),
                        entry.getValue()));
    return Stream.of(Stream.of(everyOperation), Stream.of(everyUntaggedOperation), taggedOperations)
        .flatMap(identity());
  }

  private Stream<TagsOperationAndAst> evaluatePathItem(String relativePath, PathItem pathItem) {
    var inheritedParameters = pathItem.parameters();

    return pathItem.operationsMap().entrySet().stream()
        .map(
            entry -> {
              var method = entry.getKey();
              var operation = entry.getValue();
              return OasOperationToAst.evaluateOperaton(
                  basePackage, relativePath, method, operation, inheritedParameters);
            });
  }
}
