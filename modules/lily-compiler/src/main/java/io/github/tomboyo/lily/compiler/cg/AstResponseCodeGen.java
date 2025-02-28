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

  /**
   * Get the native HTTP response from the request.
   */
  public java.net.http.HttpResponse<? extends java.io.InputStream> httpResponse() {
    return this.httpResponse;
  }

  {{#bodyReturnTypeName}}
  /**
   * Return the deserialized representation of the response body if possible. The body
   * is deserialized lazily; if this method is never called, the body is never
   * deserialized.
   *
   * @return The deserialized response body.
   * @throws java.io.IOException If the response body cannot be deserialized for any
   *         reason.
   */
  public {{{bodyReturnTypeName}}} body() throws java.io.IOException {
    {{! List<Foo>.class is not valid java, so we use an Array type intermediary. }}
    {{#bodyListReturnTypeName}}
      return java.util.Arrays.asList(objectMapper.readValue(httpResponse.body(), {{{bodyListReturnTypeName}}}[].class));
    {{/bodyListReturnTypeName}}
    {{^bodyListReturnTypeName}}
      return objectMapper.readValue(httpResponse.body(), {{{bodyReturnTypeName}}}.class);
    {{/bodyListReturnTypeName}}
  }
  {{/bodyReturnTypeName}}
}
""",
            "renderAstResponse",
            Map.of(
                "packageName",
                astResponse.name().packageName(),
                "typeName",
                astResponse.name().typeName(),
                "interfaceName",
                astResponse.sumTypeName(),
                "bodyReturnTypeName",
                astResponse.contentName().<Object>map(Fqn::toFqpString).orElse(false),
                "bodyListReturnTypeName",
                astResponse
                    .contentName()
                    .flatMap(Fqn::listType)
                    .<Object>map(Fqn::toFqpString)
                    .orElse(false)));

    return new Source(astResponse.name(), content);
  }
}
