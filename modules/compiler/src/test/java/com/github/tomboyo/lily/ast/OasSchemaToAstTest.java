package com.github.tomboyo.lily.ast;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstClassAlias;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;

class OasSchemaToAstTest {

  @Nested
  public class ScalarTypeTests {
    /** A list of scalar types and formats, and the java types they evaluate to. * */
    private static Stream<Arguments> scalars() {
      return Stream.of(
          arguments("boolean", null, "java.lang", "Boolean"),
          arguments("integer", null, "java.math", "BigInteger"),
          arguments("integer", "int32", "java.lang", "Integer"),
          arguments("integer", "int64", "java.lang", "Long"),
          arguments("number", null, "java.math", "BigDecimal"),
          arguments("number", "double", "java.lang", "Double"),
          arguments("number", "float", "java.lang", "Float"),
          arguments("string", null, "java.lang", "String"),
          arguments("string", "password", "java.lang", "String"),
          arguments("string", "byte", "java.lang", "Byte[]"),
          arguments("string", "binary", "java.lang", "Byte[]"),
          arguments("string", "date", "java.time", "LocalDate"),
          arguments("string", "date-time", "java.time", "OffsetDateTime"));
    }

    // Fields
    @ParameterizedTest
    @MethodSource("scalars")
    public void scalarObjectProperties(
        String type, String format, String javaPackage, String javaClass) {
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
                  List.of(new AstField(new AstReference(javaPackage, javaClass), "myField")))),
          ast,
          "Scalar object properties evaluate to standard java type fields");
    }

    @ParameterizedTest
    @CsvSource({
      "boolean, java.lang, Boolean",
      "integer, java.math, BigInteger",
      "number, java.math, BigDecimal",
      "string, java.lang, String"
    })
    public void unsupportedScalarObjectPropertyFormats(
        String type, String javaPackage, String javaClass) {
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
                  List.of(new AstField(new AstReference(javaPackage, javaClass), "myField")))),
          ast,
          "Unsupported formats evaluate to default (fall-back) types");

      // Warn the user when an unsupported format is found.
      verify(logger)
          .warn(
              eq("Using default class for unsupported format: type={} format={}"),
              eq(type),
              eq("unsupported-format"));
    }

    // Aliases
    @ParameterizedTest
    @MethodSource("scalars")
    public void componentsOfScalarType(
        String type, String format, String javaPackage, String javaClass) {
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

    // Lists
    @ParameterizedTest
    @MethodSource("scalars")
    public void inLineArraysWithScalarItems(
        String type, String format, String javaPackage, String javaClass) {
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
                              List.of(new AstReference(javaPackage, javaClass))),
                          "myField")))),
          ast,
          "In-line arrays of scalar items evaluate to Lists with type parameters");
    }
  }

  // Array aliases
  @Nested
  public class ComponentsWithArrayType {
    @Test
    public void arraysOfRefs() {
      var ast =
          OasSchemaToAst.generateAst(
                  "com.foo",
                  new Components()
                      .schemas(
                          Map.of(
                              "MyAlias",
                              new ArraySchema()
                                  .items(new Schema().$ref("#/components/schemas/MyComponent")))))
              .collect(toSet());

      assertEquals(
          Set.of(
              new AstClassAlias(
                  "com.foo",
                  "MyAlias",
                  new AstReference(
                      "java.util", "List", List.of(new AstReference("com.foo", "MyComponent"))))),
          ast,
          "Array components of $refs evaluate to aliases of lists of the referenced type");
    }

    @Test
    public void arrayOfStandardType() {
      var ast =
          OasSchemaToAst.generateAst(
                  "com.foo",
                  new Components()
                      .schemas(
                          Map.of(
                              "MyAlias", new ArraySchema().items(new Schema<>().type("string")))))
              .collect(toSet());

      assertEquals(
          Set.of(
              new AstClassAlias(
                  "com.foo",
                  "MyAlias",
                  new AstReference(
                      "java.util", "List", List.of(new AstReference("java.lang", "String"))))),
          ast,
          "Array components of strings evaluate to aliases of lists of strings");
    }

    @Test
    public void arrayOfInlineObjects() {
      var ast =
          OasSchemaToAst.generateAst(
                  "com.foo",
                  new Components()
                      .schemas(
                          Map.of(
                              "MyAlias",
                              new ArraySchema()
                                  .items(
                                      new ObjectSchema()
                                          .properties(
                                              Map.of("foo", new Schema().type("string")))))))
              .collect(toSet());

      assertEquals(
          Set.of(
              new AstClassAlias(
                  "com.foo",
                  "MyAlias",
                  new AstReference(
                      "java.util",
                      "List",
                      List.of(new AstReference("com.foo.myalias", "MyAliasItem")))),
              new AstClass(
                  "com.foo.myalias",
                  "MyAliasItem",
                  List.of(new AstField(new AstReference("java.lang", "String"), "foo")))),
          ast,
          "Array components of inlined objects evaluate to aliases of lists of objects");
    }
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

  @Test
  public void inLineObjectDefinition() {
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
        "in-line array item definitions evaluate to references to new classes in nested packages");
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
