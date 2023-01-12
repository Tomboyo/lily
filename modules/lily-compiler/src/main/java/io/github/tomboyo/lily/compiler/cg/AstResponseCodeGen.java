package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;

import io.github.tomboyo.lily.compiler.ast.AstResponse;
import java.util.Map;

public class AstResponseCodeGen {
  public static Source renderAstResponse(AstResponse astResponse) {
    var content =
        writeString(
            """
            package {{packageName}};

            public record {{typeName}}(
                java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse) implements {{interfaceName}} {
              public static {{typeName}} fromHttpResponse(
                  java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse) {
                return new {{typeName}}(httpResponse);
              }
            }
            """,
            "renderAstResponse",
            Map.of(
                "packageName", astResponse.name().packageName(),
                "typeName", astResponse.name().typeName(),
                "interfaceName", astResponse.sumTypeName()));

    return new Source(astResponse.name(), content);
  }
}
