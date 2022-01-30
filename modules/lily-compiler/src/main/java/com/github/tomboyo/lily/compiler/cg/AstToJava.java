package com.github.tomboyo.lily.compiler.cg;

import static com.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;
import static com.github.tomboyo.lily.compiler.icg.Support.lowerCamelCase;

import com.github.tomboyo.lily.compiler.ast.Ast;
import com.github.tomboyo.lily.compiler.ast.AstClass;
import com.github.tomboyo.lily.compiler.ast.AstClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstField;
import com.github.tomboyo.lily.compiler.ast.AstOperation;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClass;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstReference;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class AstToJava {
  public static Source renderAst(Ast ast) {
    if (ast instanceof AstClass astClass) {
      return renderClass(astClass);
    } else if (ast instanceof AstClassAlias astClassAlias) {
      return renderAstClassAlias(astClassAlias);
    } else if (ast instanceof AstOperationsClass astOperationsClass) {
      return renderAstOperationClass(astOperationsClass);
    } else if (ast instanceof AstOperationsClassAlias astOperationsClassAlias) {
      return renderAstOperationClassAlias(astOperationsClassAlias);
    } else {
      throw new IllegalArgumentException("Unsupported AST: " + ast);
    }
  }

  private static Source renderClass(AstClass ast) {
    var path =
        Path.of(".", ast.packageName().split("\\."))
            .normalize()
            .resolve(capitalCamelCase(ast.name()) + ".java");
    var content =
        """
        package %s;
        public record %s(
        %s
        ) {}"""
            .formatted(
                ast.packageName(),
                capitalCamelCase(ast.name()),
                recordFieldDeclaration(ast.fields()));

    return new Source(path, content);
  }

  private static String recordFieldDeclaration(List<AstField> fields) {
    return fields.stream()
        .map(
            field ->
                "    %s %s"
                    .formatted(
                        fullyQualifiedType(field.astReference()), lowerCamelCase(field.name())))
        .collect(Collectors.joining(",\n"));
  }

  private static String fullyQualifiedType(AstReference astReference) {
    var fqn =
        String.join(".", astReference.packageName(), capitalCamelCase(astReference.className()));

    if (astReference.typeParameters().isEmpty()) {
      return fqn;
    } else {
      var typeParameters =
          "<%s>"
              .formatted(
                  astReference.typeParameters().stream()
                      .map(AstToJava::fullyQualifiedType)
                      .collect(Collectors.joining(",")));
      return fqn + typeParameters;
    }
  }

  private static Source renderAstClassAlias(AstClassAlias ast) {
    var path =
        Path.of(".", ast.packageName().split("\\."))
            .normalize()
            .resolve(capitalCamelCase(ast.name()) + ".java");
    var valueType = fullyQualifiedType(ast.aliasedType());
    var className = capitalCamelCase(ast.name());
    var content =
        """
        package %s;
        public record %s(
            %s value
        ) {
          @com.fasterxml.jackson.annotation.JsonCreator
          public static %s creator(%s value) { return new %s(value); }
          @com.fasterxml.jackson.annotation.JsonValue
          public %s value() { return value; }
        }"""
            .formatted(
                ast.packageName(),
                className,
                valueType,
                className,
                valueType,
                className,
                valueType);
    return new Source(path, content);
  }

  private static Source renderAstOperationClass(AstOperationsClass ast) {
    var path =
        Path.of(".", ast.packageName().split("\\."))
            .normalize()
            .resolve(capitalCamelCase(ast.name()) + ".java");
    var content =
        """
        package {package};
        public class {className} {
          private {className}() {}

          {operations}
        }
        """
            .replaceAll("\\{package\\}", ast.packageName())
            .replaceAll("\\{className\\}", capitalCamelCase(ast.name()))
            .replaceAll(
                "\\{operations\\}",
                ast.operations().stream()
                    .map(AstToJava::renderOperation)
                    .collect(Collectors.joining("\n")));

    return new Source(path, content);
  }

  private static String renderOperation(AstOperation ast) {
    return """
      public static com.github.tomboyo.lily.http.HttpHelper<{responseType}> {operationName}(
        com.fasterxml.jackson.databind.ObjectMapper objectMapper,
        java.net.http.HttpClient httpClient
      ) {
        return new com.github.tomboyo.lily.http.HttpHelper(
          httpClient,
          new com.github.tomboyo.lily.http.JacksonBodyHandler(
            objectMapper,
            new com.fasterxml.jackson.core.type.TypeReference<{responseType}>() {}
          ),
          // TODO: parameterize builder
          java.net.http.HttpRequest.newBuilder()
        );
      }"""
        .replaceAll("\\{operationName\\}", lowerCamelCase(ast.id()))
        // TODO: use actual response type
        .replaceAll("\\{responseType\\}", "String");
  }

  private static Source renderAstOperationClassAlias(AstOperationsClassAlias ast) {
    var path =
        Path.of(".", ast.packageName().split("\\."))
            .normalize()
            .resolve(capitalCamelCase(ast.name()) + ".java");
    var content =
        """
        package {package};
        public class {className} {
          private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
          private final java.net.http.HttpClient httpClient;

          public {className}(
              com.fasterxml.jackson.databind.ObjectMapper objectMapper,
              java.net.http.HttpClient httpClient
          ) {
            this.objectMapper = objectMapper;
            this.httpClient = httpClient;
          }

          {methods}
        }
        """
            .replaceAll("\\{package\\}", ast.packageName())
            .replaceAll("\\{className\\}", capitalCamelCase(ast.name()))
            .replaceAll(
                "\\{methods\\}",
                ast.aliasedOperations().stream()
                    .map(operation -> renderOperationAlias(ast, operation))
                    .collect(Collectors.joining("\n")));

    return new Source(path, content);
  }

  private static String renderOperationAlias(
      AstOperationsClassAlias alias, AstOperation operation) {
    return """
        public com.github.tomboyo.lily.http.HttpHelper<{responseType}> {operationName}() {
          return {operationsClass}.{operationName}(this.objectMapper, this.httpClient);
        }
        """
        // TODO: use actual response type
        .replaceAll("\\{responseType\\}", "String")
        .replaceAll("\\{operationName\\}", lowerCamelCase(operation.id()))
        .replaceAll("\\{operationsClass\\}", fullyQualifiedType(alias.operationsSingleton()));
  }
}
