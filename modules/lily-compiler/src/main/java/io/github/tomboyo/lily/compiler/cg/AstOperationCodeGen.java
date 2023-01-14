package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.FORM;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import java.util.Map;
import java.util.stream.Collectors;

public class AstOperationCodeGen {
  public static Source renderAstOperation(AstOperation ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {
              private final io.github.tomboyo.lily.http.UriTemplate uriTemplate;
              private final java.net.http.HttpClient httpClient;
              private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

              public {{className}}(
                  String uri,
                  java.net.http.HttpClient httpClient,
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
                // We assume uri is non-null and ends with a trailing '/'.
                this.uriTemplate = io.github.tomboyo.lily.http.UriTemplate.of(uri + "{{{relativePath}}}{{{queryTemplate}}}");
                this.httpClient = httpClient;
                this.objectMapper = objectMapper;
              }

              {{#urlParameters}}
              private {{{fqpt}}} {{name}};
              public {{className}} {{name}}({{{fqpt}}} {{name}}) {
                this.{{name}} = {{name}};
                return this;
              }
              {{/urlParameters}}

              public io.github.tomboyo.lily.http.UriTemplate uriTemplate() {
                {{#smartFormEncoder}}
                var smartFormEncoder = io.github.tomboyo.lily.http.encoding.Encoders.smartFormExploded(); // stateful
                {{/smartFormEncoder}}
                {{#urlParameters}}
                if (this.{{name}} != null) {
                  uriTemplate.bind(
                      "{{apiName}}",
                      this.{{name}},
                      {{{encoder}}});
                }
                {{/urlParameters}}
                return uriTemplate;
              }

              /**
               * Return an HttpRequest which may be sent directly or further customized with the
               * {@link java.net.http.HttpRequest#newBuilder(java.net.http.HttpRequest, java.util.function.BiPredicate)}} static
               * function.
               */
              public java.net.http.HttpRequest httpRequest() {
                return java.net.http.HttpRequest.newBuilder()
                  .uri(uriTemplate().toURI())
                  .method("{{method}}", java.net.http.HttpRequest.BodyPublishers.noBody())
                  .build();
              }

              /**
               * Synchronously perform the HTTP request for this operation.
               */
              public {{{responseTypeName}}} sendSync() throws java.io.IOException, InterruptedException {
                var httpResponse = this.httpClient.send(
                  httpRequest(),
                  java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                return {{{responseTypeName}}}.fromHttpResponse(httpResponse, objectMapper);
              }
            }
            """,
            "renderAstOperation",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "className",
                ast.name().typeName(),
                "relativePath",
                withoutLeadingSlash(ast.relativePath()),
                "method",
                ast.method(),
                "queryTemplate",
                ast.parameters().stream()
                    .filter(parameter -> parameter.location() == QUERY)
                    .map(parameter -> "{" + parameter.apiName() + "}")
                    .collect(Collectors.joining("")),
                "smartFormEncoder",
                ast.parameters().stream().anyMatch(parameter -> parameter.location() == QUERY),
                // path and query parameters -- anything in the URL itself
                "urlParameters",
                ast.parameters().stream()
                    .filter(
                        parameter -> parameter.location() == PATH || parameter.location() == QUERY)
                    .map(
                        parameter ->
                            Map.of(
                                "fqpt", parameter.typeName().toFqpString(),
                                "name", parameter.name().lowerCamelCase(),
                                "apiName", parameter.apiName(),
                                "encoder", getEncoder(parameter.encoding())))
                    .collect(toList()),
                "responseTypeName",
                ast.responseName().toFqpString()));

    return new Source(ast.name(), content);
  }

  private static String withoutLeadingSlash(String path) {
    if (path.startsWith("/")) {
      return path.substring(1);
    } else {
      return path;
    }
  }

  private static String getEncoder(ParameterEncoding encoding) {
    if (encoding.style() == FORM) {
      // use the stateful smartFormEncoder local variable.
      return "smartFormEncoder";
    }

    if (encoding.explode()) {
      return "io.github.tomboyo.lily.http.encoding.Encoders.simpleExplode()";
    } else {
      return "io.github.tomboyo.lily.http.encoding.Encoders.simple()";
    }
  }
}
