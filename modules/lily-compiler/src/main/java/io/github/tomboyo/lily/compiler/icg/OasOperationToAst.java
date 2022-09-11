package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OasOperationToAst {

  private final PackageName basePackage;

  private OasOperationToAst(PackageName basePackage) {
    this.basePackage = basePackage;
  }

  public static TagsOperationAndAst evaluateOperaton(
      PackageName basePackage,
      String relativePath,
      Operation operation,
      List<Parameter> inheritedParameters) {
    return new OasOperationToAst(basePackage)
        .evaluateOperation(relativePath, operation, inheritedParameters);
  }

  private TagsOperationAndAst evaluateOperation(
      String relativePath, Operation operation, List<Parameter> inheritedParameters) {
    var operationName = SimpleName.of(operation.getOperationId()).resolve("operation");
    var subordinatePackageName = basePackage.resolve(operationName.toString());
    var ownParameters = requireNonNullElse(operation.getParameters(), List.<Parameter>of());

    var parametersAndAst =
        mergeParameters(inheritedParameters, ownParameters).stream()
            .map(
                parameter -> OasParameterToAst.evaluateParameter(subordinatePackageName, parameter))
            .collect(Collectors.toList());
    var ast =
        parametersAndAst.stream()
            .flatMap(OasParameterToAst.ParameterAndAst::ast)
            .collect(Collectors.toSet());
    var parameters =
        parametersAndAst.stream()
            .map(OasParameterToAst.ParameterAndAst::parameter)
            .collect(Collectors.toList());

    return new TagsOperationAndAst(
        getOperationTags(operation),
        new AstOperation(
            SimpleName.of(operation.getOperationId()),
            new AstReference(Fqn.of(basePackage, operationName), List.of(), false, false),
            relativePath,
            parameters),
        ast);
  }

  /** Merge owned parameters with inherited parameters. Owned parameters take precedence. */
  private static Collection<Parameter> mergeParameters(
      List<Parameter> inherited, List<Parameter> owned) {
    return Stream.concat(inherited.stream(), owned.stream())
        .collect(
            toMap(
                param -> new ParameterId(param.getName(), param.getIn()), identity(), (a, b) -> b))
        .values();
  }

  private static Set<String> getOperationTags(Operation operation) {
    var set = new HashSet<>(requireNonNullElse(operation.getTags(), List.of()));
    if (set.isEmpty()) {
      set.add("other");
    }
    set.add("all");
    return set;
  }

  /** Holds the tags, AstOperation, and other Ast from evaluating an OAS Operation. */
  public static record TagsOperationAndAst(
      Set<String> tags, AstOperation operation, Set<Ast> ast) {}

  private static record ParameterId(String name, String in) {}
}
