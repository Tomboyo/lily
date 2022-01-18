package com.github.tomboyo.lily.icg;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.github.tomboyo.lily.ast.Ast;
import com.github.tomboyo.lily.ast.AstOperation;
import com.github.tomboyo.lily.ast.AstOperationsClass;
import com.github.tomboyo.lily.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.ast.AstReference;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OasPathsToAst {

  private final String basePackage;

  private OasPathsToAst(String basePackage) {
    this.basePackage = basePackage;
  }

  private record Pair<K, V>(K key, V value) {}

  public static Stream<Ast> evaluate(String basePackage, Map<String, PathItem> paths) {
    return new OasPathsToAst(basePackage).evaluate(paths);
  }

  private Stream<Ast> evaluate(Map<String, PathItem> paths) {
    return paths.entrySet().stream().flatMap(entry -> evaluate(entry.getValue()));
  }

  private Stream<Ast> evaluate(PathItem pathItem) {
    var operations =
        pathItem.readOperations().stream().map(OasPathsToAst::evaluate).collect(Collectors.toSet());

    var operationsByTag =
        operations.stream()
            .flatMap(operation -> operation.tags().stream().map(tag -> new Pair<>(tag, operation)))
            .collect(groupingBy(Pair::key, mapping(Pair::value, toSet())));
    var aliases =
        operationsByTag.entrySet().stream()
            .map(
                entry ->
                    new AstOperationsClassAlias(
                        basePackage,
                        entry.getKey(),
                        new AstReference(basePackage, "Operations"),
                        entry.getValue()));

    return Stream.concat(
        Stream.of(new AstOperationsClass(basePackage, "Operations", operations)), aliases);
  }

  private static AstOperation evaluate(Operation operation) {
    Set<String> tags;
    if (operation.getTags() != null) {
      tags = new HashSet<>(operation.getTags());
    } else {
      tags = Set.of();
    }

    return new AstOperation(tags, operation.getOperationId());
  }
}
