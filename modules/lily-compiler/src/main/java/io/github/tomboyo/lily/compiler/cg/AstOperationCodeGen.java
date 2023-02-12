package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.FORM;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.SIMPLE;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.HEADER;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import java.util.Map;
import java.util.stream.Collectors;

public class AstOperationCodeGen {

  private static String ENCODERS = "io.github.tomboyo.lily.http.encoding.Encoders";

  public static Source renderAstOperation(AstOperation ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {
              private final String baseUri;
              private final io.github.tomboyo.lily.http.UriTemplate pathTemplate;
              private final io.github.tomboyo.lily.http.UriTemplate queryTemplate;
              private final java.net.http.HttpClient httpClient;
              private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

              private Query query;
              private Path path;
              private Headers headers;
              {{#bodyFqpt}}
              private {{{bodyFqpt}}} body;
              {{/bodyFqpt}}

              public {{className}}(
                  String baseUri,
                  java.net.http.HttpClient httpClient,
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
                // We assume uri is non-null and ends with a trailing '/'.
                this.baseUri = baseUri;
                this.pathTemplate = io.github.tomboyo.lily.http.UriTemplate.of("{{{pathTemplate}}}");
                this.queryTemplate = io.github.tomboyo.lily.http.UriTemplate.of("{{{queryTemplate}}}");
                this.httpClient = httpClient;
                this.objectMapper = objectMapper;

                this.path = new Path();
                this.query = new Query();
                this.headers = new Headers();
              }

              /** Configure path parameters for this operation, if any. */
              public {{className}} path(java.util.function.Function<Path, Path> path) {
                this.path = path.apply(this.path);
                return this;
              }

              /** Configure query parameters for this operation, if any. */
              public {{className}} query(java.util.function.Function<Query, Query> query) {
                this.query = query.apply(this.query);
                return this;
              }

              /** Configure headers for this operation, if any. */
              public {{className}} headers(java.util.function.Function<Headers, Headers> headers) {
                this.headers = headers.apply(this.headers);
                return this;
              }

              {{#bodyFqpt}}
              /** Configure the request body. */
              public {{className}} body({{{bodyFqpt}}} body) {
                this.body = body;
                return this;
              }
              {{/bodyFqpt}}


              /** Get the base URI of the service (like {@code "https://example.com/"}). It always
                * ends with a trailing slash.
                */
              public String baseUri() {
                return this.baseUri;
              }

              /** Get this operation's relative path interpolated with any bound parameters. The
                * path is always relative, so it does not start with a "/".
                */
              public String pathString() {
                {{#pathSmartFormEncoder}}
                var smartFormEncoder = io.github.tomboyo.lily.http.encoding.Encoders.smartFormExploded();
                {{/pathSmartFormEncoder}}
                return this.pathTemplate
                {{#pathParameters}}
                  .bind("{{apiName}}", this.path.{{name}}, {{{encoder}}})
                {{/pathParameters}}
                  .toString();
              }

              /** Get the query string for this operation and any bound parameters. */
              public String queryString() {
                {{#querySmartFormEncoder}}
                var smartFormEncoder = io.github.tomboyo.lily.http.encoding.Encoders.smartFormExploded();
                {{/querySmartFormEncoder}}
                return this.queryTemplate
                {{#queryParameters}}
                  .bind("{{apiName}}", this.query.{{name}}, {{{encoder}}})
                {{/queryParameters}}
                  .toString();
              }

              /**
               * Return an HttpRequest which may be sent directly or further customized with the
               * {@link java.net.http.HttpRequest#newBuilder(java.net.http.HttpRequest, java.util.function.BiPredicate)}} static
               * function.
               */
              public java.net.http.HttpRequest httpRequest() {{#bodyFqpt}}throws com.fasterxml.jackson.core.JsonProcessingException{{/bodyFqpt}} {
                return java.net.http.HttpRequest.newBuilder()
                  .uri(java.net.URI.create(this.baseUri + pathString() + queryString()))
                  .method(
                      "{{method}}",
                      {{#bodyFqpt}}
                      this.body == null
                          ? java.net.http.HttpRequest.BodyPublishers.noBody()
                          : java.net.http.HttpRequest.BodyPublishers.ofByteArray(
                              this.objectMapper.writeValueAsBytes(this.body))
                      {{/bodyFqpt}}
                      {{^bodyFqpt}}
                      java.net.http.HttpRequest.BodyPublishers.noBody()
                      {{/bodyFqpt}})
                   {{#bodyFqpt}}
                   .header("content-type", "application/json")
                   {{/bodyFqpt}}
                   {{#headers}}
                   .header("{{apiName}}", {{{encoder}}}.encode("{{apiName}}", this.headers.{{name}}))
                   {{/headers}}
                  .build();
              }

              /**
               * Synchronously perform the HTTP request for this operation.
               */
              public {{{responseTypeName}}} sendSync() throws java.io.IOException, InterruptedException {
                return sendSync(httpRequest());
              }

              /**
               * Synchronously perform the HTTP request for a custom HttpRequest. You will typically
               * only use this API when the underlying OpenAPI specification is missing parameters
               * or other necessary components. Use the {@link #httpRequest()} method to get a
               * template HTTP request from this operation, customize it with
               * {@link java.net.http.HttpRequest#newBuilder(java.net.http.HttpRequest, java.util.function.BiPredicate)},
               * then use this method to dispatch it.
               */
              public {{{responseTypeName}}} sendSync(java.net.http.HttpRequest request)
                  throws java.io.IOException, InterruptedException {
                var httpResponse = this.httpClient.send(
                  request,
                  java.net.http.HttpResponse.BodyHandlers.ofInputStream());
                return {{{responseTypeName}}}.fromHttpResponse(httpResponse, objectMapper);
              }

              public static class Path {
                private Path() {}

                {{#pathParameters}}
                private {{{fqpt}}} {{name}};
                public Path {{name}}({{{fqpt}}} {{name}}) {
                  this.{{name}} = {{name}};
                  return this;
                }
                {{/pathParameters}}
              }

              public static class Query {
                private Query() {}

                {{#queryParameters}}
                private {{{fqpt}}} {{name}};
                public Query {{name}}({{{fqpt}}} {{name}}) {
                  this.{{name}} = {{name}};
                  return this;
                }
                {{/queryParameters}}
              }

              public static class Headers {
                private Headers() {}

                {{#headers}}
                private {{{fqpt}}} {{name}};
                public Headers {{name}}({{{fqpt}}} {{name}}) {
                  this.{{name}} = {{name}};
                  return this;
                }
                {{/headers}}
              }
            }
            """,
            "renderAstOperation",
            Map.ofEntries(
                entry("packageName", ast.name().packageName()),
                entry("className", ast.name().typeName()),
                entry("pathTemplate", withoutLeadingSlash(ast.relativePath())),
                entry(
                    "queryTemplate",
                    ast.parameters().stream()
                        .filter(parameter -> parameter.location() == QUERY)
                        .map(parameter -> "{" + parameter.apiName() + "}")
                        .collect(Collectors.joining(""))),
                entry("method", ast.method()),
                entry(
                    "pathSmartFormEncoder",
                    ast.parameters().stream()
                        .anyMatch(
                            parameter ->
                                parameter.location() == PATH
                                    && parameter.encoding().style() == FORM)),
                entry(
                    "querySmartFormEncoder",
                    ast.parameters().stream()
                        .anyMatch(
                            parameter ->
                                parameter.location() == QUERY
                                    && parameter.encoding().style() == FORM)),
                entry(
                    "pathParameters",
                    ast.parameters().stream()
                        .filter(parameter -> parameter.location() == PATH)
                        .map(
                            parameter ->
                                Map.of(
                                    "fqpt", parameter.typeName().toFqpString(),
                                    "name", parameter.name().lowerCamelCase(),
                                    "apiName", parameter.apiName(),
                                    "encoder", getEncoderForUri(parameter.encoding())))
                        .collect(toList())),
                entry(
                    "queryParameters",
                    ast.parameters().stream()
                        .filter(parameter -> parameter.location() == QUERY)
                        .map(
                            parameter ->
                                Map.of(
                                    "fqpt", parameter.typeName().toFqpString(),
                                    "name", parameter.name().lowerCamelCase(),
                                    "apiName", parameter.apiName(),
                                    "encoder", getEncoderForUri(parameter.encoding())))
                        .collect(toList())),
                entry("responseTypeName", ast.responseName().toFqpString()),
                entry("bodyFqpt", ast.requestBody().<Object>map(Fqn::toFqpString).orElse(false)),
                entry(
                    "headers",
                    ast.parameters().stream()
                        .filter(parameter -> parameter.location() == HEADER)
                        .map(
                            parameter ->
                                Map.of(
                                    "fqpt", parameter.typeName().toFqpString(),
                                    "name", parameter.name().lowerCamelCase(),
                                    "apiName", parameter.apiName(),
                                    "encoder", getEncoderForHeaders(parameter.encoding())))
                        .toList())));

    return new Source(ast.name(), content);
  }

  private static String withoutLeadingSlash(String path) {
    if (path.startsWith("/")) {
      return path.substring(1);
    } else {
      return path;
    }
  }

  private static String getEncoderForUri(ParameterEncoding encoding) {
    if (encoding.style() == FORM && encoding.explode()) {
      // use the stateful smartFormEncoder local variable.
      return "smartFormEncoder";
    }

    if (encoding.style() == SIMPLE && !encoding.explode()) {
      return ENCODERS + ".simple()";
    }

    throw new RuntimeException("Unsupported encoding: " + encoding);
  }

  private static String getEncoderForHeaders(ParameterEncoding encoding) {
    if (encoding.style() == FORM && encoding.explode()) {
      return ENCODERS + ".formExploded()";
    }

    if (encoding.style() == SIMPLE && !encoding.explode()) {
      return ENCODERS + ".simple()";
    }

    throw new RuntimeException("Unsupported encoding: " + encoding);
  }
}
