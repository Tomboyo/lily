package com.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.github.tomboyo.lily.compiler.ast.Ast;
import com.github.tomboyo.lily.compiler.ast.AstApi;
import com.github.tomboyo.lily.compiler.ast.AstOperation;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClass;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstReference;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
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
    var operations =
        paths.entrySet().stream()
            .flatMap(entry -> evaluate(entry.getKey(), entry.getValue()))
            .collect(toSet());

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
                        entry.getValue()))
            .collect(toSet());

    var api =
        new AstApi(
            basePackage,
            "Api",
            aliases.stream()
                .map(alias -> new AstReference(alias.packageName(), alias.name()))
                .collect(toSet()));

    return Stream.concat(
        Stream.of(api, new AstOperationsClass(basePackage, "Operations", operations)),
        aliases.stream());
  }

  private Stream<AstOperation> evaluate(String path, PathItem pathItem) {
    return pathItem.readOperationsMap().entrySet().stream()
        .map(entry -> evaluate(path, entry.getKey(), entry.getValue()));
  }

  private static AstOperation evaluate(String path, HttpMethod httpMethod, Operation operation) {
    Set<String> tags;
    if (operation.getTags() != null) {
      tags = new HashSet<>(operation.getTags());
    } else {
      tags = Set.of("defaultTag");
    }

    var method =
        switch (httpMethod) {
          case DELETE -> AstOperation.Method.DELETE;
          case GET -> AstOperation.Method.GET;
          case HEAD -> AstOperation.Method.HEAD;
          case OPTIONS -> AstOperation.Method.OPTIONS;
          case PATCH -> AstOperation.Method.PATCH;
          case POST -> AstOperation.Method.POST;
          case PUT -> AstOperation.Method.PUT;
          case TRACE -> AstOperation.Method.TRACE;
        };

    requireNonNull(operation.getOperationId(), "TODO: support paths without operations IDs");
    return new AstOperation(tags, operation.getOperationId(), method, path);
  }
}
