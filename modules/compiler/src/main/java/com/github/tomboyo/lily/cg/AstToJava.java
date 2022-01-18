package com.github.tomboyo.lily.cg;

import static com.github.tomboyo.lily.icg.Support.capitalCamelCase;
import static com.github.tomboyo.lily.icg.Support.lowerCamelCase;

import com.github.tomboyo.lily.ast.Ast;
import com.github.tomboyo.lily.ast.AstClass;
import com.github.tomboyo.lily.ast.AstClassAlias;
import com.github.tomboyo.lily.ast.AstField;
import com.github.tomboyo.lily.ast.AstOperation;
import com.github.tomboyo.lily.ast.AstOperationsClass;
import com.github.tomboyo.lily.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.ast.AstReference;
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
        package %s;
        public class %s {
          %s
        }
        """
            .formatted(
                ast.packageName(),
                capitalCamelCase(ast.name()),
                ast.operations().stream()
                    .map(AstToJava::renderOperation)
                    .collect(Collectors.joining("\n  ")));

    return new Source(path, content);
  }

  private static String renderOperation(AstOperation ast) {
    return "public static Void %s() { return null; }".formatted(lowerCamelCase(ast.id()));
  }

  private static Source renderAstOperationClassAlias(AstOperationsClassAlias ast) {
    var path =
        Path.of(".", ast.packageName().split("\\."))
            .normalize()
            .resolve(capitalCamelCase(ast.name()) + ".java");
    var content =
        """
        package %s;
        public class %s {
          %s
        }
        """
            .formatted(
                ast.packageName(),
                capitalCamelCase(ast.name()),
                ast.aliasedOperations().stream()
                    .map(operation -> renderOperationAlias(ast, operation))
                    .collect(Collectors.joining("\n  ")));

    return new Source(path, content);
  }

  private static String renderOperationAlias(
      AstOperationsClassAlias alias, AstOperation operation) {
    return "public static Void %s() { return %s.%s(); }"
        .formatted(
            lowerCamelCase(operation.id()),
            fullyQualifiedType(alias.operationsSingleton()),
            lowerCamelCase(operation.id()));
  }
}
