package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.Ast;
import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;

import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.github.tomboyo.lily.ast.Support.capitalCamelCase;
import static com.github.tomboyo.lily.ast.Support.lowerCamelCase;

public class AstToJava {
  public static Source renderAst(Ast ast) {
    return switch (ast) {
      case AstClass astClass -> renderClass(astClass);
      default -> new Source(
          Path.of(".").normalize(),
          "TODO: This AST is unimplemented: " + ast);
    };
  }

  private static Source renderClass(AstClass ast) {
    var path = Path.of(".", ast.packageName().split("\\."))
        .normalize()
        .resolve(capitalCamelCase(ast.name()) + ".java");
    var content = """
        package %s;
        public class %s {
        %s
        %s
        }""".formatted(
        ast.packageName(),
        capitalCamelCase(ast.name()),
        ast.fields().stream()
            .map(AstToJava::renderFieldDeclaration)
            .collect(Collectors.joining("\n")),
        ast.fields().stream()
            .map(astField -> renderGetterAndSetter(ast, astField))
            .collect(Collectors.joining("\n")));

    return new Source(path, content);
  }

  private static String renderFieldDeclaration(AstField astField) {
    return "private %s %s;".formatted(
        renderTypeName(astField.astReference()),
        astField.name());
  }

  private static String renderGetterAndSetter(AstClass astClass, AstField astField) {
    var typeName = renderTypeName(astField.astReference());
    var getterName = lowerCamelCase(astField.name());
    var setterName = lowerCamelCase(astField.name());
    var fieldName = lowerCamelCase(astField.name());
    var className = capitalCamelCase(astClass.name());

    var getter = "public %s %s() { return %s; }"
        .formatted(
            typeName,
            getterName,
            fieldName);

    var setter = "public %s %s(%s %s) { this.%s = %s; return this; }"
        .formatted(
            className,
            setterName,
            typeName,
            fieldName,
            fieldName,
            fieldName);

    return String.join("\n", getter, setter);
  }

  private static String renderTypeName(AstReference astReference) {
    var fqn = String.join(
        ".",
        astReference.packageName(),
        capitalCamelCase(astReference.className()));

    if (astReference.typeParameters().isEmpty()) {
      return fqn;
    } else {
      var typeParameters = "<%s>".formatted(
          astReference.typeParameters().stream()
              .map(AstToJava::renderTypeName)
              .collect(Collectors.joining(",")));
      return fqn + typeParameters;
    }
  }
}
