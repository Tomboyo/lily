package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.Ast;
import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;

import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.github.tomboyo.lily.ast.Support.capitalCamelCase;
import static com.github.tomboyo.lily.ast.Support.lowerCamelCase;

public class AstToJava {
  public static Source renderAst(Ast ast) {
    return switch (ast) {
      case AstClass astClass -> renderClass(astClass);
      default -> new Source(Path.of(".").normalize(), "");
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

  private static String renderFieldDeclaration(AstField ast) {
    var fqn = String.join(".",
        ast.astReference().packageName(),
        capitalCamelCase(ast.astReference().className()));
    return "private %s %s;".formatted(fqn, ast.name());
  }

  private static String renderGetterAndSetter(AstClass astClass, AstField astField) {
    var fqFieldType = String.join(".",
        astField.astReference().packageName(),
        capitalCamelCase(astField.astReference().className()));
    var getterName = lowerCamelCase(astField.name());
    var setterName = lowerCamelCase(astField.name());
    var fieldName = lowerCamelCase(astField.name());
    var className = capitalCamelCase(astClass.name());

    var getter = "public %s %s() { return %s; }"
        .formatted(
            fqFieldType,
            getterName,
            fieldName);

    var setter = "public %s %s(%s %s) { this.%s = %s; return this; }"
        .formatted(
            className,
            setterName,
            fqFieldType,
            fieldName,
            fieldName,
            fieldName);

    return String.join("\n", getter, setter);
  }
}
