package com.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.github.tomboyo.lily.compiler.ast.Ast;
import com.github.tomboyo.lily.compiler.ast.AstApi;
import com.github.tomboyo.lily.compiler.ast.AstOperation;
import com.github.tomboyo.lily.compiler.ast.AstReference;
import com.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    var tagsAndOperations =
        paths.entrySet().stream().flatMap(entry -> evaluate(entry.getValue())).collect(toSet());

    var operations = tagsAndOperations.stream().map(Pair::value);
    var taggedOperations =
        tagsAndOperations.stream()
            .flatMap(pair -> pair.key().stream().map(tag -> new Pair<>(tag, pair.value)))
            .collect(groupingBy(Pair::key, mapping(Pair::value, toSet())))
            .entrySet()
            .stream()
            .map(
                entry ->
                    new AstTaggedOperations(
                        basePackage, entry.getKey() + "Operations", entry.getValue()))
            .collect(toSet());

    return Stream.concat(
        Stream.concat(operations, taggedOperations.stream()),
        Stream.of(new AstApi(basePackage, "Api", taggedOperations)));
  }

  private Stream<Pair<Set<String>, AstOperation>> evaluate(PathItem pathItem) {
    return pathItem.readOperationsMap().entrySet().stream()
        .map(entry -> evaluate(entry.getValue()));
  }

  private Pair<Set<String>, AstOperation> evaluate(Operation operation) {
    Set<String> tags;
    if (operation.getTags() != null) {
      tags = new HashSet<>(operation.getTags());
    } else {
      tags = Set.of("other");
    }

    requireNonNull(operation.getOperationId(), "Operations must have a unique non-null ID");
    return new Pair(
        tags,
        new AstOperation(
            operation.getOperationId(),
            new AstReference(basePackage, operation.getOperationId() + "Operation")));
  }
}
