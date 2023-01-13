package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;

import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.Map;

public class AstResponseCodeGen {
  public static Source renderAstResponse(AstResponse astResponse) {
    var content =
        writeString(
            """
            package {{packageName}};

            public non-sealed class {{typeName}} implements {{interfaceName}} {

              private final java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse;
              private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

              public {{typeName}}(
                  java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse,
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
                this.httpResponse = httpResponse;
                this.objectMapper = objectMapper;
              }

              public static {{typeName}} fromHttpResponse(
                  java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse,
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper)
                      throws java.io.IOException {
                return new {{typeName}}(httpResponse, objectMapper);
              }

              public java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse() {
                return this.httpResponse;
              }

              {{#contentTypeName}}
              public {{{contentTypeName}}} body() throws java.io.IOException {
                return objectMapper.readValue(httpResponse.body(), {{{contentTypeName}}}.class);
              }
              {{/contentTypeName}}
            }
            """,
            "renderAstResponse",
            Map.of(
                "packageName", astResponse.name().packageName(),
                "typeName", astResponse.name().typeName(),
                "interfaceName", astResponse.sumTypeName(),
                "contentTypeName",
                    astResponse.contentName().<Object>map(Fqn::toFqpString).orElse(false)));

    return new Source(astResponse.name(), content);
  }
}
