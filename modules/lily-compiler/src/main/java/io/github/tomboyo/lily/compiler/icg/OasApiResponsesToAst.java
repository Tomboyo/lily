package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class OasApiResponsesToAst {

  public static Stream<Ast> evaluateApiResponses(
      PackageName basePackage, SimpleName operationId, ApiResponses responses) {
    var genRoot = basePackage.resolve(operationId.resolve("operation")); // a.b.getfoooperation
    var responseSumName = Fqn.newBuilder(genRoot, operationId.resolve("Response")).build();

    var memberAsts =
        responses.entrySet().stream()
            .map(
                entry -> {
                  var statusCode = entry.getKey();
                  var response = entry.getValue();
                  var astResponseName = operationId.resolve(statusCode);
                  return evaluateApiResponse(
                      basePackage, genRoot, astResponseName, responseSumName, response);
                })
            .toList();
    var members = memberAsts.stream().map(Pair::left).toList();
    var ast = memberAsts.stream().flatMap(Pair::right);

    var sum = new AstResponseSum(responseSumName, members);

    return Stream.concat(Stream.of(sum), ast);
  }

  private static Pair<Fqn, Stream<Ast>> evaluateApiResponse(
      PackageName basePackage,
      PackageName operationPackage,
      SimpleName responseName,
      Fqn sumTypeName,
      ApiResponse response) {
    // gen into com.example.myoperation.response
    var responsePackage = operationPackage.resolve("response");

    var contentFqnAndAst =
        evaluateApiResponseContent(basePackage, responsePackage, responseName, response);

    var astHeadersAndAst =
        evaluateApiResponseHeaders(basePackage, responsePackage, responseName, response);

    var astResponse =
        new AstResponse(
            Fqn.newBuilder(operationPackage, responseName).build(),
            astHeadersAndAst.map(Pair::left).map(AstHeaders::name),
            contentFqnAndAst.map(Pair::left),
            sumTypeName);

    return new Pair<>(
        astResponse.name(),
        Stream.of(
                Stream.of(astResponse),
                astHeadersAndAst.map(Pair::left).stream(),
                contentFqnAndAst.map(Pair::right).stream().flatMap(identity()),
                astHeadersAndAst.stream().flatMap(Pair::right))
            .flatMap(identity()));
  }

  private static Optional<Pair<AstHeaders, Stream<Ast>>> evaluateApiResponseHeaders(
      PackageName basePackage,
      PackageName responsePackage,
      SimpleName responseName,
      ApiResponse response) {
    if (response.getHeaders() == null || response.getHeaders().isEmpty()) {
      return Optional.empty();
    }

    var headersName = Fqn.newBuilder(responsePackage, responseName.resolve("Headers")).build();
    var headersFieldsAndAst =
        requireNonNullElse(response.getHeaders(), Map.<String, Header>of()).entrySet().stream()
            .map(
                entry -> {
                  var name = entry.getKey();
                  var header = entry.getValue();
                  if (header.getSchema() != null) {
                    return OasSchemaToAst.evaluateInto(
                            basePackage,
                            headersName.toPackage(),
                            SimpleName.of(name).resolve("Header"),
                            header.getSchema())
                        .mapLeft(fqn -> new Field(fqn, SimpleName.of(name), name));
                  } else {
                    var fqn =
                        OasSchemaToAst.fqnForRef(basePackage, requireNonNull(header.get$ref()));
                    return new Pair<>(new Field(fqn, SimpleName.of(name), name), Stream.<Ast>of());
                  }
                })
            .toList();

    var astHeaders =
        new AstHeaders(headersName, headersFieldsAndAst.stream().map(Pair::left).toList());

    return Optional.of(new Pair<>(astHeaders, headersFieldsAndAst.stream().flatMap(Pair::right)));
  }

  private static Optional<Pair<Fqn, Stream<Ast>>> evaluateApiResponseContent(
      PackageName basePackage,
      PackageName responsePackage,
      SimpleName responseName,
      ApiResponse apiResponse) {
    return Optional.ofNullable(apiResponse.getContent())
        .map(content -> content.get("application/json"))
        .map(MediaType::getSchema)
        .map(
            schema ->
                OasSchemaToAst.evaluateInto(
                    basePackage, responsePackage, responseName.resolve("Content"), schema));
  }
}
