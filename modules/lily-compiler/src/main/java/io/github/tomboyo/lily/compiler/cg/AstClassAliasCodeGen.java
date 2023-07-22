package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;

import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.Fqn;

import java.util.Map;
import java.util.stream.Collectors;

public class AstClassAliasCodeGen {
  public static Source renderAstClassAlias(AstClassAlias ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fqpValueName}}} value
            ) {{implementsClause}} {
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
                "fqpValueName", ast.aliasedType().toFqpString(),
                "implementsClause",
                implementsClause(ast)));

    return new Source(ast.name(), content);
  }

  private static String implementsClause(AstClassAlias ast) {
    return "implements " + ast.interfaces().stream()
        .map(Fqn::toFqpString)
        .collect(Collectors.joining(", "));
  }
}
