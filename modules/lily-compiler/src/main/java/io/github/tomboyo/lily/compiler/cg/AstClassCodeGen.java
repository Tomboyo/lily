package io.github.tomboyo.lily.compiler.cg;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.Field;

import java.util.Map;
import java.util.stream.Collectors;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astByteBuffer;

public class AstClassCodeGen {
  public static Source renderClass(AstClass ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fields}}}
            ) {}
            """,
            "renderClass",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "recordName",
                ast.name().typeName().upperCamelCase(),
                "fields",
                ast.fields().stream()
                    .map(AstClassCodeGen::recordField)
                    .collect(Collectors.joining(",\n"))));

    return new Source(ast.name(), content);
  }

  private static String recordField(Field field) {
    var scope =
        Map.of(
            "fqpt", field.astReference().toFqpString(),
            "name", field.name().lowerCamelCase(),
            "jsonName", field.jsonName());

    if (field.astReference().equals(astByteBuffer())) {
      // Byte buffers will deser as B64 strings by default, which is not compliant with the OpenAPI
      // specification, so we add custom deser.
      return writeString(
          """
          @com.fasterxml.jackson.annotation.JsonProperty("{{jsonName}}")
          @com.fasterxml.jackson.databind.annotation.JsonSerialize(
              using=io.github.tomboyo.lily.http.deser.ByteBufferSerializer.class)
          @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
              using=io.github.tomboyo.lily.http.deser.ByteBufferDeserializer.class)
          {{{fqpt}}} {{name}}
          """,
          "recordFieldByteBuffer",
          scope);
    } else {
      return writeString(
          """
          @com.fasterxml.jackson.annotation.JsonProperty("{{jsonName}}")
          {{{fqpt}}} {{name}}
          """,
          "recordField",
          scope);
    }
  }
}
