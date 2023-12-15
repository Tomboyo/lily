package io.github.tomboyo.lily.compiler.cg.support;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static io.github.tomboyo.lily.compiler.icg.StdlibFqns.astByteBuffer;

import io.github.tomboyo.lily.compiler.ast.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Fields {

  private static final Set<String> reservedWords =
      Set.of(
          "abstract",
          "continue",
          "for",
          "new",
          "switch",
          "assert",
          "default",
          "goto",
          "package",
          "synchronized",
          "boolean",
          "do",
          "if",
          "private",
          "this",
          "break",
          "double",
          "implements",
          "protected",
          "throw",
          "byte",
          "else",
          "import",
          "public",
          "throws",
          "case",
          "enum",
          "instanceof",
          "return",
          "transient",
          "catch",
          "extends",
          "int",
          "short",
          "try",
          "char",
          "final",
          "interface",
          "static",
          "void",
          "class",
          "finally",
          "long",
          "strictfp",
          "volatile",
          "const",
          "float",
          "native",
          "super",
          "while",
          "true",
          "false",
          "null");

  public static String recordFields(Collection<Field> fields) {
    return fields.stream().map(Fields::field).collect(Collectors.joining(",\n"));
  }

  public static String getterNameForField(Field field) {
    if (reservedWords.contains(field.name().lowerCamelCase())) {
      /* This rule is only necessary for "class", which becomes get_Class() so as not to override getClass()
      (which cannot be overridden). We apply the same rule for other reserved words to be consistent, so that this
      naming deviation is easier to remember. Words like "null" become get_Null(). */
      return "get_" + field.name().upperCamelCase();
    } else {
      return "get" + field.name().upperCamelCase();
    }
  }

  public static String fieldName(Field field) {
    var name = field.name().lowerCamelCase();
    if (reservedWords.contains(name)) {
      return "_" + name;
    }
    return name;
  }

  private static String field(Field field) {
    var scope =
        Map.of(
            "fqpt", field.astReference().toFqpString(),
            "name", fieldName(field),
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
          "AstClassCodeGen.recordField.byteBuffer",
          scope);
    } else {
      return writeString(
          """
                        @com.fasterxml.jackson.annotation.JsonProperty("{{jsonName}}")
                        {{{fqpt}}} {{name}}
                        """,
          "AstClassCodeGen.recordField",
          scope);
    }
  }
}
