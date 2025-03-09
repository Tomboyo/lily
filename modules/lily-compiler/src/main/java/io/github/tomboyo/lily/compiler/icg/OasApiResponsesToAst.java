package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.oas.model.*;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.github.tomboyo.lily.compiler.util.Triple;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OasApiResponsesToAst {

  public static Pair<Fqn, Stream<Ast>> evaluateApiResponses(
      PackageName basePackage, SimpleName operationId, Responses responses) {
    var genRoot = basePackage.resolve(operationId.resolve("operation")); // a.b.getfoooperation
    var responseSumName = Fqn.newBuilder(genRoot, operationId.resolve("Response")).build();

    var memberAsts =
        Stream.concat(
                responses.responseMap().entrySet().stream(),
                // Lily needs something to hold unexpected responses.
                responses.responseMap().containsKey("default")
                    ? Stream.of()
                    : Stream.of(Map.entry("default", Response.empty())))
            .map(
                entry -> {
                  var statusCode = entry.getKey();
                  var response = entry.getValue();
                  var astResponseName = operationId.resolve(statusCode);
                  var evaluated =
                      evaluateApiResponse(
                          basePackage, genRoot, astResponseName, responseSumName, response);
                  return new Triple<>(statusCode, evaluated.left(), evaluated.right());
                })
            .toList();
    var members = memberAsts.stream().collect(Collectors.toMap(Triple::first, Triple::second));
    var ast = memberAsts.stream().flatMap(Triple::third);

    var sum = new AstResponseSum(responseSumName, members);

    return new Pair<>(responseSumName, Stream.concat(Stream.of(sum), ast));
  }

  private static Pair<Fqn, Stream<Ast>> evaluateApiResponse(
      PackageName basePackage,
      PackageName operationPackage,
      SimpleName responseName,
      Fqn sumTypeName,
      IResponse iResponse) {
    // TODO: handle Refs
    // gen into com.example.myoperation.response
    var responsePackage = operationPackage.resolve("response");

    Optional<Pair<Fqn, Stream<Ast>>> contentFqnAndAst =
        switch (iResponse) {
          case None none -> Optional.empty();
          // TODO: correctly handle $refs
          case Ref ref -> Optional.empty();
          case Response response ->
              evaluateApiResponseContent(basePackage, responsePackage, responseName, response);
        };

    Optional<Pair<AstHeaders, Stream<Ast>>> astHeadersAndAst =
        switch (iResponse) {
          case None none -> Optional.empty();
          // TODO: correctly handle $refs
          case Ref ref -> Optional.empty();
          case Response response ->
              evaluateApiResponseHeaders(basePackage, responsePackage, responseName, response);
        };

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
      Response response) {
    if (response.headers().isEmpty()) {
      return Optional.empty();
    }

    var headersName = Fqn.newBuilder(responsePackage, responseName.resolve("Headers")).build();
    var headersFieldsAndAst =
        response.headers().entrySet().stream()
            .flatMap(
                entry -> {
                  var name = entry.getKey();
                  var iHeader = entry.getValue();
                  return switch (iHeader) {
                    case None none -> Stream.of();
                    case Ref(String $ref) -> {
                      var fqn = OasSchemaToAst.fqnForRef(basePackage, $ref);
                      // TODO: issues/98
                      yield Stream.of(
                          new Pair<>(
                              new Field(fqn, SimpleName.of(name), name, false), Stream.<Ast>of()));
                    }
                    case Header header ->
                        switch (header.schema()) {
                          case None none -> Stream.of();
                          // TODO: handle $ref correctly
                          case Ref ref -> Stream.of();
                          case Schema schema ->
                              Stream.of(
                                  OasSchemaToAst.evaluateInto(
                                          basePackage,
                                          headersName.toPackage(),
                                          SimpleName.of(name).resolve("Header"),
                                          schema)
                                      // TODO: issues/98
                                      .mapLeft(
                                          fqn -> new Field(fqn, SimpleName.of(name), name, false)));
                        };
                  };
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
      // TODO: handle Ref?
      Response responseDef) {
    return Optional.ofNullable(responseDef.content())
        .map(content -> content.get("application/json"))
        .map(MediaType::schema)
        .map(
            schema ->
                OasSchemaToAst.evaluateInto(
                    basePackage, responsePackage, responseName.resolve("Content"), schema));
  }
}
