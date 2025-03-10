package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.Map;
import java.util.stream.Collectors;

public class AstResponseSumCodeGen {
  public static Source renderAstResponseSum(AstResponseSum astResponseSum) {
    var content =
        writeString(
            """
package {{packageName}};

public sealed interface {{typeName}} permits {{members}} {

  /** Access the native java.net.http.HttpResponse describing the result of an operation. */
  public java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse();

  public static {{typeName}} fromHttpResponse(
      java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper)
          throws java.io.IOException {
    return switch(httpResponse.statusCode()) {
      {{#statusCodeToMember}}
      case {{statusCode}} -> {{memberName}}.fromHttpResponse(httpResponse, objectMapper);
      {{/statusCodeToMember}}
      default -> {{{defaultMember}}};
    };
  }
};
""",
            "renderAstResponseSum",
            Map.of(
                "packageName",
                astResponseSum.name().packageName(),
                "typeName",
                astResponseSum.name().typeName(),
                "statusCodeToMember",
                astResponseSum.statusCodeToMember().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("default"))
                    .map(
                        entry ->
                            Map.of("statusCode", entry.getKey(), "memberName", entry.getValue()))
                    .collect(toList()),
                "defaultMember",
                requireNonNull(astResponseSum.statusCodeToMember().get("default"))
                    + ".fromHttpResponse(httpResponse, objectMapper)",
                "members",
                astResponseSum.statusCodeToMember().values().stream()
                    .map(Fqn::toFqString)
                    .collect(Collectors.joining(", "))));

    return new Source(astResponseSum.name(), content);
  }
}
