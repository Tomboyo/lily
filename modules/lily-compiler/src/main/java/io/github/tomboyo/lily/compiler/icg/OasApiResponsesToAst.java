package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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

    var contentSchema = response.getContent().get("application/json").getSchema();
    var contentName = responseName.resolve("Content");
    var contentFqnAndAst =
        OasSchemaToAst.evaluateInto(basePackage, responsePackage, contentName, contentSchema);

    //    var headersFqnAndAst = response.getHeaders().entrySet().stream()
    //        .map(entry -> {
    //          var name = entry.getKey();
    //          var header = entry.getValue();
    //          return OasSchemaToAst.evaluateInto(
    //              basePackage,
    //              responsePackage,
    //              responseName.resolve(name).resolve("Header"),
    //              header.getSchema());
    //        });

    var headersName = Fqn.newBuilder(responsePackage, responseName.resolve("Headers")).build();
    var headersAst = new AstHeaders(headersName);

    var astResponse =
        new AstResponse(
            Fqn.newBuilder(operationPackage, responseName).build(),
            headersName,
            // Use the evaluated Fqn in case the schema was actually e.g. a $ref or a List<Foo>
            contentFqnAndAst.left(),
            sumTypeName);

    return new Pair<>(
        astResponse.name(),
        Stream.concat(Stream.of(astResponse, headersAst), contentFqnAndAst.right()));
  }
}
