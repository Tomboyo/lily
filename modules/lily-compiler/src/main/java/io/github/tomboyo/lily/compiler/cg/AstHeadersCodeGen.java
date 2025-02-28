package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;

import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.Field;
import java.util.Map;
import java.util.stream.Collectors;

public class AstHeadersCodeGen {
  public static Source renderAstHeaders(AstHeaders astHeaders) {
    var content =
        writeString(
            """
            package {{packageName}};

            public record {{{typeName}}}(
              {{{recordFields}}}
            ) {}
            """,
            "AstHeadersCodeGen.renderAstHeaders",
            Map.of(
                "packageName",
                astHeaders.name().packageName(),
                "typeName",
                astHeaders.name().typeName(),
                "recordFields",
                astHeaders.fields().stream()
                    .map(AstHeadersCodeGen::recordField)
                    .collect(Collectors.joining(","))));
    return new Source(astHeaders.name(), content);
  }

  private static String recordField(Field field) {
    var scope =
        Map.of(
            "typeName", field.astReference().toFqpString(),
            "name", field.name().lowerCamelCase());
    return writeString(
        """
        {{{typeName}}} {{name}}
        """,
        "AstHeadersCodeGen.recordField",
        scope);
  }
}
