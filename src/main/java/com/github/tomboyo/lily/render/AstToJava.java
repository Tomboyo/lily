package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.Ast;
import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;

import java.util.stream.Collectors;

import static com.github.tomboyo.lily.ast.Support.capitalCamelCase;
import static com.github.tomboyo.lily.ast.Support.lowerCamelCase;
import static com.github.tomboyo.lily.ast.Support.toClassCase;

public class AstToJava {
  public static String renderAst(String packageName, Ast ast) {
    return switch (ast) {
      case AstClass astClass -> renderClass(packageName, astClass);
      default -> "";
    };
  }

  private static String renderClass(String packageName, AstClass ast) {
    return """
        package %s;
        public class %s {
        %s
        %s
        }""".formatted(
            packageName,
            toClassCase(ast.name()),
            ast.fields().stream()
                .map(AstToJava::renderFieldDeclaration)
                .collect(Collectors.joining("\n")),
            ast.fields().stream()
                .map(astField -> renderGetterAndSetter(ast, astField))
                .collect(Collectors.joining("\n")));
  }

  private static String renderFieldDeclaration(AstField ast) {
    var fqn = String.join(".",
        ast.astReference().packageName(),
        toClassCase(ast.astReference().className()));
    return "private %s %s;".formatted(fqn, ast.name());
  }

  private static String renderGetterAndSetter(AstClass astClass, AstField astField) {
    var fqFieldType = String.join(".",
        astField.astReference().packageName(),
        toClassCase(astField.astReference().className()));
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
