package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AstApi;
import java.util.Map;

public class AstApiToJavaSource {
  public static Source renderAstAPi(AstApi ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {

              private final String uri;
              private final java.net.http.HttpClient httpClient;

              private {{className}}(String uri, java.net.http.HttpClient httpClient) {
                java.util.Objects.requireNonNull(uri);
                java.util.Objects.requireNonNull(httpClient);

                if (uri.endsWith("/")) {
                  this.uri = uri;
                } else {
                  this.uri = uri + "/";
                }

                this.httpClient = httpClient;
              }

              public static {{className}}Builder newBuilder() {
                return new {{className}}Builder();
              }

              {{#tags}}
              {{! Note: Tag types are never parameterized }}
              public {{fqReturnType}} {{methodName}}() {
                return new {{fqReturnType}}(this.uri, this.httpClient);
              }

              {{/tags}}

              /**
               * Get the underlying client used to construct this Api.
               *
               * @return the underlying HttpClient.
               */
              public java.net.http.HttpClient httpClient() {
                return httpClient;
              }

              public static class {{className}}Builder {
                private String uri;
                private java.net.http.HttpClient httpClient;

                private {{className}}Builder() {
                  httpClient = java.net.http.HttpClient.newBuilder().build();
                }

                /**
                 * Configure the base URL for all requests.
                 *
                 * @param uri The base URL for all requests.
                 * @return This builder for chaining.
                 */
                public {{className}}Builder uri(String uri) { this.uri = uri; return this; }

                /**
                 * Configure the builder with a particular client.
                 *
                 * <p/> By default, clients are equal to {@code HttpClient.newBuilder().build()}.
                 *
                 * @param httpClient a particular client to use for requests.
                 * @return This builder for chaining.
                 */
                public {{className}}Builder httpClient(java.net.http.HttpClient httpClient) {
                  this.httpClient = httpClient;
                  return this;
                }

                public {{className}} build() {
                  return new {{className}}(uri, httpClient);
                }
              }
            }
            """,
            "renderAstApi",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "className",
                ast.name().typeName().upperCamelCase(),
                "tags",
                ast.taggedOperations().stream()
                    .map(
                        tag ->
                            Map.of(
                                "fqReturnType", tag.name().toString(),
                                "methodName", tag.name().typeName().lowerCamelCase()))
                    .collect(toList())));

    return new Source(ast.name(), content);
  }
}
