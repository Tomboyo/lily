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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class OasPathsToAst {

  private final String basePackage;

  private OasPathsToAst(String basePackage) {
    this.basePackage = basePackage;
  }

  public static Stream<Ast> evaluate(String basePackage, Map<String, PathItem> paths) {
    return new OasPathsToAst(basePackage).evaluate(paths);
  }

  private Stream<Ast> evaluate(Map<String, PathItem> paths) {
    var results =
        paths.entrySet().stream().flatMap(entry -> evaluate(entry.getValue())).collect(toSet());

    var operations = results.stream().map(Result::operation);

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
                        basePackage, entry.getKey() + "Operations", entry.getValue()))
            .collect(toSet());

    return Stream.of(
            operations,
            taggedOperations.stream(),
            results.stream().flatMap(Result::ast),
            Stream.of(new AstApi(basePackage, "Api", taggedOperations)))
        .flatMap(identity());
  }

  private Stream<Result> evaluate(PathItem pathItem) {
    return pathItem.readOperationsMap().entrySet().stream()
        .map(
            entry ->
                evaluate(
                    entry.getValue(), requireNonNullElse(pathItem.getParameters(), List.of())));
  }

  private Result evaluate(Operation operation, List<Parameter> inheritedParameters) {
    var packageName = joinPackages(basePackage, operation.getOperationId());
    var ast =
        resolveParameters(
                inheritedParameters, requireNonNullElse(operation.getParameters(), List.of()))
            .stream()
            .flatMap(parameter -> evaluateParameter(packageName, parameter));

    return new Result(
        getOperationTags(operation),
        new AstOperation(
            operation.getOperationId(),
            new AstReference(
                basePackage, operation.getOperationId() + "Operation", List.of(), false)),
        ast);
  }

  private static Set<String> getOperationTags(Operation operation) {
    if (operation.getTags() != null) {
      return new HashSet<>(operation.getTags());
    } else {
      return Set.of("other");
    }
  }

  private static Collection<Parameter> resolveParameters(
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

  private static record Result(Set<String> tags, AstOperation operation, Stream<Ast> ast) {}

  private static record ParameterId(String name, String in) {}
}
