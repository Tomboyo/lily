package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.Ast;
import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomboyo.lily.ast.Support.capitalCamelCase;
import static com.github.tomboyo.lily.ast.Support.lowerCamelCase;

public class AstToJava {
  public static Source renderAst(Ast ast) {
    if (ast instanceof AstClass astClass) {
      return renderClass(astClass);
    } else {
      return new Source(
          Path.of(".").normalize(),
          "TODO: This AST is unimplemented: " + ast);
    }
  }

  private static Source renderClass(AstClass ast) {
    var path = Path.of(".", ast.packageName().split("\\."))
        .normalize()
        .resolve(capitalCamelCase(ast.name()) + ".java");
    var content = """
        package %s;
        public record %s(
        %s
        ) {}""".formatted(
        ast.packageName(),
        capitalCamelCase(ast.name()),
        recordFieldDeclaration(ast.fields()));

    return new Source(path, content);
  }

  private static String recordFieldDeclaration(List<AstField> fields) {
    return fields.stream()
        .map(field -> "    %s %s".formatted(
            typeName(field.astReference()),
            lowerCamelCase(field.name())
        ))
        .collect(Collectors.joining(",\n"));
  }

  private static String typeName(AstReference astReference) {
    var fqn = String.join(
        ".",
        astReference.packageName(),
        capitalCamelCase(astReference.className()));

    if (astReference.typeParameters().isEmpty()) {
      return fqn;
    } else {
      var typeParameters = "<%s>".formatted(
          astReference.typeParameters().stream()
              .map(AstToJava::typeName)
              .collect(Collectors.joining(",")));
      return fqn + typeParameters;
    }
  }
}
