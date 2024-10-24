package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static io.github.tomboyo.lily.compiler.cg.support.Interfaces.implementsClause;

import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import java.util.Map;

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
                "packageName",
                ast.name().packageName(),
                "recordName",
                ast.name().typeName().upperCamelCase(),
                "fqpValueName",
                ast.aliasedType().toFqpString(),
                "implementsClause",
                implementsClause(ast)));

    return new Source(ast.name(), content);
  }
}
