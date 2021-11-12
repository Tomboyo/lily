package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstReference;
import com.github.tomboyo.lily.ast.type.Field;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OasSchemaToAstTest {

  @Nested
  public class AstClassFields {

    @ParameterizedTest
    @CsvSource({
      "boolean,, java.lang, Boolean",
      "integer,, java.math, BigInteger",
      "integer, int32, java.lang, Integer",
      "integer, int64, java.lang, Long",
      "number,, java.math, BigDecimal",
      "number, double, java.lang, Double",
      "number, float, java.lang, Float",
      "string,, java.lang, String",
      "string, password, java.lang, String",
      "string, byte, java.lang, Byte[]",
      "string, binary, java.lang, Byte[]",
      "string, date, java.time, LocalDate",
      "string, date-time, java.time, ZonedDateTime"
    })
    public void standardTypesAndFormats(
        String type, String format, String packageName, String className) {
      var schema =
          new Schema()
              .name("MyComponent")
              .type("object")
              .properties(Map.of("myField", new Schema().type(type).format(format)));

      var ast =
          (AstClass)
              OasSchemaToAst.generateAst("com.foo", "MyComponent", schema).findAny().orElseThrow();

      assertEquals(
          new AstClass(
              "MyComponent",
              List.of(new Field(new AstReference(packageName, className), "myField"))),
          ast,
          "OAS primitives evaluate to standard java types");
    }

    @ParameterizedTest
    @CsvSource({
      "boolean, java.lang, Boolean",
      "string, java.lang, String",
      "integer, java.math, BigInteger",
      "number, java.math, BigDecimal",
      "string, java.lang, String"
    })
    public void unsupportedFormats(String type, String expectedPackage, String expectedClass) {
      Logger logger = mock(Logger.class);

      var ast =
          OasSchemaToAst.generateAst(
                  logger,
                  "com.foo",
                  "MyComponent",
                  new Schema()
                      .name("MyComponent")
                      .type("object")
                      .properties(
                          Map.of("myField", new Schema().type(type).format("unsupported-format"))))
              .findAny()
              .orElseThrow();

      assertEquals(
          new AstClass(
              "MyComponent",
              List.of(new Field(new AstReference(expectedPackage, expectedClass), "myField"))),
          ast,
          "Unsupported formats fall back to default types");

      // Warn the user when an unsupported format is found.
      verify(logger)
          .warn(
              eq("Using default class for unsupported format: type={} format={}"),
              eq(type),
              eq("unsupported-format"));
    }

    @Test
    public void unsupportedTypes() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              OasSchemaToAst.generateAst(
                  "com.foo",
                  "MyComponent",
                  new Schema()
                      .name("MyComponent")
                      .type("object")
                      .properties(Map.of("myField", new Schema().type("unsupported-type")))),
          "Unsupported types trigger runtime exceptions.");
    }
  }
}
