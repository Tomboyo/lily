package io.github.tomboyo.lily.compiler.cg;

import java.util.Map;
import java.util.stream.Collectors;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;

import io.github.tomboyo.lily.compiler.ast.AstInterface;
import io.github.tomboyo.lily.compiler.ast.Fqn;

public class AstInterfaceCodeGen {
    public static Source renderInterface(AstInterface ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            
            public sealed interface {{interfaceName}} permits
                {{permits}} {}
            """,
            "renderInterface",
            Map.<String, Object>of(
                "packageName",
                ast.name().packageName(),
                "interfaceName",
                ast.name().typeName().upperCamelCase(),
                "permits",
                ast.permits().stream()
                        .map(Fqn::toFqpString)
                        .collect(Collectors.joining(","))));

    return new Source(ast.name(), content);
  }
}
