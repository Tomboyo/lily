package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import java.util.Map;

public class AstTaggedOperationToJavaSource {
  public static Source renderAstTaggedOperations(AstTaggedOperations ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {

              private final String uri;
              private final java.net.http.HttpClient httpClient;

              public {{className}}(
                  String uri,
                  java.net.http.HttpClient httpClient) {
                // Assumed non-null and to end with a trailing '/'.
                this.uri = uri;
                this.httpClient = httpClient;
              }

              {{#operations}}
              public {{{fqReturnType}}} {{methodName}}() {
                return new {{{fqReturnType}}}(this.uri, this.httpClient);
              }

              {{/operations}}
            }
            """,
            "renderAstTaggedOperations",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "className",
                ast.name().typeName().upperCamelCase(),
                "operations",
                ast.operations().stream()
                    .map(
                        operation ->
                            Map.of(
                                "fqReturnType", operation.name().toFqpString(),
                                "methodName", operation.operationName().lowerCamelCase()))
                    .collect(toList())));

    return new Source(ast.name(), content);
  }
}
