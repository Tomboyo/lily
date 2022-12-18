package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.FORM;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astByteBuffer;
import static java.util.stream.Collectors.toList;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstApi;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

public class AstToJava {

  private final MustacheFactory mustacheFactory;

  private AstToJava(MustacheFactory mustacheFactory) {
    this.mustacheFactory = mustacheFactory;
  }

  private <T> String writeString(String template, String name, T scopes) {
    var mustache = mustacheFactory.compile(new StringReader(template), name);
    var stringWriter = new StringWriter();
    mustache.execute(stringWriter, scopes);
    return stringWriter.toString();
  }

  public static Source renderAst(Ast ast) {
    var self = new AstToJava(new DefaultMustacheFactory());

    if (ast instanceof AstApi astApi) {
      return self.renderAstAPi(astApi);
    } else if (ast instanceof AstClass astClass) {
      return self.renderClass(astClass);
    } else if (ast instanceof AstClassAlias astClassAlias) {
      return self.renderAstClassAlias(astClassAlias);
    } else if (ast instanceof AstHeaders astHeaders) {
      return self.renderAstHeaders(astHeaders);
    } else if (ast instanceof AstOperation astOperation) {
      return self.renderAstOperation(astOperation);
    } else if (ast instanceof AstResponseSum astResponseSum) {
      return self.renderAstResponseSum(astResponseSum);
    } else if (ast instanceof AstResponse astResponse) {
      return self.renderAstResponse(astResponse);
    } else if (ast instanceof AstTaggedOperations astTaggedOperations) {
      return self.renderAstTaggedOperations(astTaggedOperations);
    } else {
      throw new IllegalArgumentException("Unsupported AST: " + ast);
    }
  }

  private Source renderClass(AstClass ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fields}}}
            ) {}
            """,
            "renderClass",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "recordName",
                ast.name().typeName().upperCamelCase(),
                "fields",
                ast.fields().stream().map(this::recordField).collect(Collectors.joining(",\n"))));

    return createSource(ast.name(), content);
  }

  private String recordField(Field field) {
    var scope =
        Map.of(
            "fqpt", field.astReference().toFqpString(),
            "name", field.name().lowerCamelCase(),
            "jsonName", field.jsonName());

    if (field.astReference().equals(astByteBuffer())) {
      // Byte buffers will deser as B64 strings by default, which is not compliant with the OpenAPI
      // specification, so we add custom deser.
      return writeString(
          """
          @com.fasterxml.jackson.annotation.JsonProperty("{{jsonName}}")
          @com.fasterxml.jackson.databind.annotation.JsonSerialize(
              using=io.github.tomboyo.lily.http.deser.ByteBufferSerializer.class)
          @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
              using=io.github.tomboyo.lily.http.deser.ByteBufferDeserializer.class)
          {{{fqpt}}} {{name}}
          """,
          "recordFieldByteBuffer",
          scope);
    } else {
      return writeString(
          """
          @com.fasterxml.jackson.annotation.JsonProperty("{{jsonName}}")
          {{{fqpt}}} {{name}}
          """,
          "recordField",
          scope);
    }
  }

  private Source renderAstClassAlias(AstClassAlias ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fqpValueName}}} value
            ) {
              @com.fasterxml.jackson.annotation.JsonCreator
              public static {{{recordName}}} creator({{{fqpValueName}}} value) { return new {{recordName}}(value); }

              @com.fasterxml.jackson.annotation.JsonValue
              public {{{fqpValueName}}} value() { return value; }
            }
            """,
            "renderAstClassAlias",
            Map.of(
                "packageName", ast.name().packageName(),
                "recordName", ast.name().typeName().upperCamelCase(),
                "fqpValueName", ast.aliasedType().toFqpString()));

    return createSource(ast.name(), content);
  }

  private Source renderAstAPi(AstApi ast) {
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
                return new {{fqReturnType}}(uri);
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
                "packageName", ast.name().packageName(),
                "className", ast.name().typeName().upperCamelCase(),
                "tags",
                    ast.taggedOperations().stream()
                        .map(
                            tag ->
                                Map.of(
                                    "fqReturnType", tag.name().toString(),
                                    "methodName", tag.name().typeName().lowerCamelCase()))
                        .collect(toList())));

    return createSource(ast.name(), content);
  }

  private Source renderAstTaggedOperations(AstTaggedOperations ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {

              private final String uri;

              public {{className}}(String uri) {
                // Assumed non-null and to end with a trailing '/'.
                this.uri = uri;
              }

              {{#operations}}
              public {{{fqReturnType}}} {{methodName}}() {
                return new {{{fqReturnType}}}(uri);
              }

              {{/operations}}
            }
            """,
            "renderAstTaggedOperations",
            Map.of(
                "packageName", ast.name().packageName(),
                "className", ast.name().typeName().upperCamelCase(),
                "operations",
                    ast.operations().stream()
                        .map(
                            operation ->
                                Map.of(
                                    "fqReturnType", operation.name().toFqpString(),
                                    "methodName", operation.operationName().lowerCamelCase()))
                        .collect(toList())));

    return createSource(ast.name(), content);
  }

  private Source renderAstOperation(AstOperation ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {
              private final io.github.tomboyo.lily.http.UriTemplate uriTemplate;

              public {{className}}(String uri) {
                // We assume uri is non-null and ends with a trailing '/'.
                this.uriTemplate = io.github.tomboyo.lily.http.UriTemplate.of(uri + "{{{relativePath}}}{{{queryTemplate}}}");
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
            }
            """,
            "renderAstOperation",
            Map.of(
                "packageName", ast.name().packageName(),
                "className", ast.name().typeName(),
                "relativePath", withoutLeadingSlash(ast.relativePath()),
                "method", ast.method(),
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
                            parameter ->
                                parameter.location() == PATH || parameter.location() == QUERY)
                        .map(
                            parameter ->
                                Map.of(
                                    "fqpt", parameter.typeName().toFqpString(),
                                    "name", parameter.name().lowerCamelCase(),
                                    "apiName", parameter.apiName(),
                                    "encoder", getEncoder(parameter.encoding())))
                        .collect(toList())));

    return createSource(ast.name(), content);
  }

  private Source renderAstResponseSum(AstResponseSum astResponseSum) {
    var content =
        writeString(
            """
        package {{packageName}};

        public sealed interface {{typeName}} permits {{members}} {};
        """,
            "renderAstResponseSum",
            Map.of(
                "packageName", astResponseSum.name().packageName(),
                "typeName", astResponseSum.name().typeName(),
                "members",
                    astResponseSum.members().stream()
                        .map(Fqn::toFqString)
                        .collect(Collectors.joining(", "))));

    return createSource(astResponseSum.name(), content);
  }

  private Source renderAstResponse(AstResponse astResponse) {
    var content =
        writeString(
            """
            package {{packageName}};

            public record {{typeName}}(
              {{{contentTypeName}}} content,
              {{{headersTypeName}}} headers
            ) implements {{interfaceName}} {}
            """,
            "renderAstResponse",
            Map.of(
                "packageName", astResponse.name().packageName(),
                "typeName", astResponse.name().typeName(),
                "headersTypeName", astResponse.headersName().toFqpString(),
                "contentTypeName", astResponse.contentName().toFqpString(),
                "interfaceName", astResponse.sumTypeName()));

    return createSource(astResponse.name(), content);
  }

  private Source renderAstHeaders(AstHeaders astHeaders) {
    var content =
        writeString(
            """
            package {{packageName}};

            public record {{{typeName}}}() {}
            """,
            "renderAstHeaders",
            Map.of(
                "packageName", astHeaders.name().packageName(),
                "typeName", astHeaders.name().typeName()));
    return createSource(astHeaders.name(), content);
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

  private static Source createSource(Fqn fqn, String content) {
    return new Source(fqn.toPath(), fqn.toString(), content);
  }

  private static String withoutLeadingSlash(String path) {
    if (path.startsWith("/")) {
      return path.substring(1);
    } else {
      return path;
    }
  }
}
