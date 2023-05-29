package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      HttpMethod method,
      Operation operation,
      List<Parameter> inheritedParameters) {
    return new OasOperationToAst(basePackage)
        .evaluateOperation(relativePath, method, operation, inheritedParameters);
  }

  private TagsOperationAndAst evaluateOperation(
      String relativePath,
      HttpMethod method,
      Operation operation,
      List<Parameter> inheritedParameters) {
    var operationId = SimpleName.of(operation.getOperationId());
    var operationName = operationId.resolve("operation");
    var subordinatePackageName = basePackage.resolve(operationName.toString());
    var ownParameters = requireNonNullElse(operation.getParameters(), List.<Parameter>of());

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
            basePackage,
            operationId,
            requireNonNullElse(operation.getResponses(), new ApiResponses()));

    return new TagsOperationAndAst(
        new HashSet<>(requireNonNullElse(operation.getTags(), List.of())),
        new AstOperation(
            SimpleName.of(operation.getOperationId()),
            Fqn.newBuilder().packageName(basePackage).typeName(operationName).build(),
            method.name(),
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

    return Optional.ofNullable(operation.getRequestBody())
        .map(RequestBody::getContent)
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(
            schema ->
                OasSchemaToAst.evaluateInto(
                    basePackage, genRoot, operationId.resolve("Body"), schema))
        .map(pair -> new Pair<>(Optional.of(pair.left()), pair.right()))
        .orElse(new Pair<>(Optional.empty(), Stream.empty()));
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

  /** Holds the tags, AstOperation, and other Ast from evaluating an OAS Operation. */
  public record TagsOperationAndAst(Set<String> tags, AstOperation operation, Set<Ast> ast) {}

  private record ParameterId(String name, String in) {}
}
