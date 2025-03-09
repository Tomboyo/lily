package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.oas.model.*;
import io.github.tomboyo.lily.compiler.util.Pair;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OasOperationToAst {

  private final PackageName basePackage;

  private OasOperationToAst(PackageName basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Evaluate an OAS Operation to AST.
   *
   * @param basePackage The root package name for all generated types.
   * @param relativePath The relative URL of the Operation.
   * @param operation The operation to evaluate.
   * @param inheritedParameters All OpenAPI parameters inherited from the parent Path Item.
   * @return A TagsOperationAndAst describing the operation and generated types.
   */
  public static TagsOperationAndAst evaluateOperaton(
      PackageName basePackage,
      String relativePath,
      String method,
      Operation operation,
      List<IParameter> inheritedParameters) {
    return new OasOperationToAst(basePackage)
        .evaluateOperation(relativePath, method, operation, inheritedParameters);
  }

  private TagsOperationAndAst evaluateOperation(
      String relativePath,
      String method,
      Operation operation,
      List<IParameter> inheritedParameters) {
    // TODO: if operationId is missing, skip this operation.
    var operationId = operation.operationId().map(SimpleName::of).orElseThrow();
    var operationName = operationId.resolve("operation");
    var subordinatePackageName = basePackage.resolve(operationName.toString());
    var ownParameters = operation.parameters();

    var parametersAndAst =
        mergeParameters(inheritedParameters, ownParameters).stream()
            .map(
                parameter ->
                    OasParameterToAst.evaluateParameter(
                        basePackage, subordinatePackageName, parameter))
            .flatMap(Optional::stream)
            .toList();
    var parameterAst = parametersAndAst.stream().flatMap(OasParameterToAst.ParameterAndAst::ast);
    var parameters =
        parametersAndAst.stream()
            .map(OasParameterToAst.ParameterAndAst::parameter)
            .collect(Collectors.toList());

    var bodyAndAst = evaluateRequestBody(operation, subordinatePackageName, operationId);

    var responseSumAndAst =
        OasApiResponsesToAst.evaluateApiResponses(
            // TODO: just deser Responses to an empty instance
            basePackage, operationId, operation.responses().orElse(new Responses(Map.of())));

    return new TagsOperationAndAst(
        // TODO: does this need to be mutable?
        new HashSet<>(operation.tags()),
        new AstOperation(
            // TODO: ignore operation if ID is missing rather than throw
            SimpleName.of(operation.operationId().orElseThrow()),
            Fqn.newBuilder().packageName(basePackage).typeName(operationName).build(),
            method,
            relativePath,
            parameters,
            bodyAndAst.left(),
            responseSumAndAst.left()),
        Stream.of(responseSumAndAst.right(), bodyAndAst.right(), parameterAst)
            .flatMap(identity())
            .collect(Collectors.toSet()));
  }

  private Pair<Optional<Fqn>, Stream<Ast>> evaluateRequestBody(
      Operation operation, PackageName genRoot, SimpleName operationId) {

    return operation
        .requestBody()
        // TODO: handle RequestBody.Ref
        .filter(RequestBody.class::isInstance)
        .map(RequestBody.class::cast)
        .map(RequestBody::content)
        .map(map -> map.get("application/json"))
        .flatMap(MediaType::schema)
        .map(
            schema ->
                OasSchemaToAst.evaluateInto(
                    basePackage, genRoot, operationId.resolve("Body"), schema))
        .map(pair -> new Pair<>(Optional.of(pair.left()), pair.right()))
        .orElse(new Pair<>(Optional.empty(), Stream.empty()));
  }

  /** Merge owned parameters with inherited parameters. Owned parameters take precedence. */
  private static Collection<IParameter> mergeParameters(
      List<IParameter> inherited, List<IParameter> owned) {
    return Stream.concat(inherited.stream(), owned.stream())
        // TODO: handle Parameters.Ref instances
        .filter(Parameter.class::isInstance)
        .map(x -> (Parameter) x)
        .filter(x -> x.name().isPresent() && x.in().isPresent())
        .collect(
            toMap(
                param -> new ParameterId(param.name().get(), param.in().get()),
                x -> (IParameter) x,
                (a, b) -> b))
        .values();
  }

  /** Holds the tags, AstOperation, and other Ast from evaluating an OAS Operation. */
  public record TagsOperationAndAst(Set<String> tags, AstOperation operation, Set<Ast> ast) {}

  private record ParameterId(String name, String in) {}
}
