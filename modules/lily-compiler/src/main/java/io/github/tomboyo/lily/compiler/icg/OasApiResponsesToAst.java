package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OasApiResponsesToAst {

  public static Pair<AstResponseSum, Stream<Ast>> evaluateApiResponses(
      PackageName basePackage, SimpleName operationId, ApiResponses responses) {
    var responseSumTypeName = Fqn.of(basePackage, operationId.resolve("Response"));
    var definitions =
        responses.entrySet().stream()
            .map(
                entry -> {
                  var responseCode = entry.getKey();
                  var apiResponse = entry.getValue();
                  return evaluateApiResponse(
                      basePackage, operationId.resolve(responseCode), apiResponse);
                })
            .flatMap(Optional::stream)
            // For each definition tree, replace the root element with an AstResponse sum type
            // member otherwise describing
            // the same class.
            .map(
                refAndAst -> {
                  var root = refAndAst.left();
                  var asts =
                      refAndAst
                          .right()
                          .map(
                              ast -> {
                                if (ast instanceof AstClass astClass
                                    && root.name().equals(astClass.name())) {
                                  return new AstResponse(
                                      root.name(), astClass.fields(), responseSumTypeName);
                                } else {
                                  return ast;
                                }
                              });
                  return new Pair<>(root, asts);
                })
            .toList();
    var members = definitions.stream().map(Pair::left).collect(Collectors.toSet());
    var memberAst = definitions.stream().flatMap(Pair::right);
    return new Pair<>(
        new AstResponseSum(responseSumTypeName, new LinkedHashSet<>(members)), memberAst);
  }

  private static Optional<Pair<AstReference, Stream<Ast>>> evaluateApiResponse(
      PackageName basePackage, SimpleName responseName, ApiResponse response) {
    return Optional.ofNullable(response.getContent())
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(schema -> OasSchemaToAst.evaluate(basePackage, responseName, schema));
  }
}
