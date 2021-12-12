package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstClassAlias;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OasSchemaToAstTest {

  private static String[][] standardTypesAndFormats() {
    return new String[][] {
      // OAS type, OAS format, expected package, expected class
      new String[] {"boolean", null, "java.lang", "Boolean"},
      new String[] {"integer", null, "java.math", "BigInteger"},
      new String[] {"integer", "int32", "java.lang", "Integer"},
      new String[] {"integer", "int64", "java.lang", "Long"},
      new String[] {"number", null, "java.math", "BigDecimal"},
      new String[] {"number", "double", "java.lang", "Double"},
      new String[] {"number", "float", "java.lang", "Float"},
      new String[] {"string", null, "java.lang", "String"},
      new String[] {"string", "password", "java.lang", "String"},
      new String[] {"string", "byte", "java.lang", "Byte[]"},
      new String[] {"string", "binary", "java.lang", "Byte[]"},
      new String[] {"string", "date", "java.time", "LocalDate"},
      new String[] {"string", "date-time", "java.time", "OffsetDateTime"}
    };
  }

  @ParameterizedTest
  @MethodSource("standardTypesAndFormats")
  public void aliasTypes(String type, String format, String javaPackage, String javaClass) {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(Map.of("MyAliasComponent", new Schema().type(type).format(format))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClassAlias(
                "com.foo", "MyAliasComponent", new AstReference(javaPackage, javaClass))),
        ast,
        "Top-level components with standard types evaluate to type aliases");
  }

  @ParameterizedTest
  @MethodSource("standardTypesAndFormats")
  public void standardTypesAndFormats(
      String type, String format, String expectedPackage, String expectedClass) {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .name("MyComponent")
                                .properties(
                                    Map.of("myField", new Schema().type(type).format(format))))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(new AstReference(expectedPackage, expectedClass), "myField")))),
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
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .name("MyComponent")
                                .properties(
                                    Map.of(
                                        "myField",
                                        new Schema().type(type).format("unsupported-format"))))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(new AstReference(expectedPackage, expectedClass), "myField")))),
        ast,
        "Unsupported formats evaluate to default (fall-back) types");

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
                    new Components()
                        .schemas(
                            Map.of(
                                "MyComponent",
                                new ObjectSchema()
                                    .name("MyComponent")
                                    .properties(
                                        Map.of("myField", new Schema().type("unsupported-type"))))))
                .toList(),
        "Unsupported types trigger runtime exceptions.");
  }

  @ParameterizedTest
  @MethodSource("standardTypesAndFormats")
  public void arrays(String type, String format, String expectedPackage, String expectedClass) {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .name("MyComponent")
                                .properties(
                                    Map.of(
                                        "myField",
                                        new ArraySchema()
                                            .type("array")
                                            .items(new Schema().type(type).format(format)))))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(
                        new AstReference(
                            "java.util",
                            "List",
                            List.of(new AstReference(expectedPackage, expectedClass))),
                        "myField")))),
        ast,
        "Arrays evaluate to Lists with type parameters");
  }

  @Test
  public void inLineObjectFieldDefinition() {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .name("MyComponent")
                                .properties(
                                    Map.of(
                                        "myField",
                                        new ObjectSchema()
                                            .properties(
                                                Map.of(
                                                    "myOtherField",
                                                    new Schema().type("string"))))))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(new AstReference("com.foo.mycomponent", "MyField"), "myField"))),
            new AstClass(
                "com.foo.mycomponent",
                "MyField",
                List.of(new AstField(new AstReference("java.lang", "String"), "myOtherField")))),
        ast,
        "in-line object definitions evaluate to references to new classes in a nested package");
  }

  @Test
  public void inLineArrayItemDefinition() {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .name("MyComponent")
                                .properties(
                                    Map.of(
                                        "myItems",
                                        new ArraySchema()
                                            .items(
                                                new ObjectSchema()
                                                    .properties(
                                                        Map.of(
                                                            "myString", new StringSchema()))))))))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(
                        new AstReference(
                            "java.util",
                            "List",
                            List.of(new AstReference("com.foo.mycomponent", "MyItemsItem"))),
                        "myItems"))),
            new AstClass(
                "com.foo.mycomponent",
                "MyItemsItem",
                List.of(new AstField(new AstReference("java.lang", "String"), "myString")))),
        ast,
        "in-line array item definitions within object definitions evaluate to references to new classes in nested packages");
  }

  @Test
  public void componentReferences() {
    var ast =
        OasSchemaToAst.generateAst(
                "com.foo",
                new Components()
                    .schemas(
                        Map.of(
                            "MyComponent",
                            new ObjectSchema()
                                .properties(
                                    Map.of(
                                        "myRef",
                                        new Schema()
                                            .$ref("#/components/schemas/MyReferencedComponent"))),
                            "MyReferencedComponent",
                            new ObjectSchema())))
            .collect(toSet());

    assertEquals(
        Set.of(
            new AstClass(
                "com.foo",
                "MyComponent",
                List.of(
                    new AstField(new AstReference("com.foo", "MyReferencedComponent"), "myRef"))),
            new AstClass("com.foo", "MyReferencedComponent", List.of())),
        ast,
        "$ref types evaluate to references to other classes");
  }
}
