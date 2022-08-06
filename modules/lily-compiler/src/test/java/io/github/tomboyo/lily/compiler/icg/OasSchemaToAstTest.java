package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astListOf;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OasSchemaToAstTest {

  /** A list of scalar types and formats, and the java types they evaluate to. * */
  public static Stream<Arguments> scalarsSource() {
    return Stream.of(
        arguments("boolean", null, "java.lang", "Boolean"),
        arguments("boolean", "unsupported-format", "java.lang", "Boolean"),
        arguments("integer", null, "java.math", "BigInteger"),
        arguments("integer", "unsupported-format", "java.math", "BigInteger"),
        arguments("integer", "int32", "java.lang", "Integer"),
        arguments("integer", "int64", "java.lang", "Long"),
        arguments("number", null, "java.math", "BigDecimal"),
        arguments("number", "unsupported-format", "java.math", "BigDecimal"),
        arguments("number", "double", "java.lang", "Double"),
        arguments("number", "float", "java.lang", "Float"),
        arguments("string", null, "java.lang", "String"),
        arguments("string", "unsupportedFormat", "java.lang", "String"),
        arguments("string", "password", "java.lang", "String"),
        arguments("string", "byte", "java.lang", "Byte[]"),
        arguments("string", "binary", "java.lang", "Byte[]"),
        arguments("string", "date", "java.time", "LocalDate"),
        arguments("string", "date-time", "java.time", "OffsetDateTime"));
  }

  @Test
  public void unsupportedSchemaTypes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> OasSchemaToAst.evaluate("p", "MyBadSchema", new Schema<>().type("unsupported-type")),
        "Unsupported types trigger runtime exceptions.");
  }

  @Nested
  class ScalarSchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluate(String oasType, String oasFormat, String javaPackage, String javaClass) {
      var actual =
          OasSchemaToAst.evaluate("p", "fieldName", new Schema().type(oasType).format(oasFormat));

      assertEquals(
          new Pair<>(new AstReference(javaPackage, javaClass, List.of(), true), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns a standard type reference and no AST since nothing was generated");
    }
  }

  @Nested
  class ObjectSchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluateWithScalarProperty(
        String oasType, String oasFormat, String javaPackage, String javaClass) {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyObject",
              new ObjectSchema()
                  .properties(Map.of("myField", new Schema().type(oasType).format(oasFormat))));

      assertEquals(
          new Pair<>(
              new AstReference("p", "MyObject", List.of(), false),
              Set.of(
                  AstClass.of(
                      "p",
                      "MyObject",
                      List.of(
                          new AstField(
                              new AstReference(javaPackage, javaClass, List.of(), true),
                              "myField"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the generated type and its AST");
    }

    @Test
    void evaluateWithInlineObjectProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyObject",
              new ObjectSchema()
                  .properties(
                      Map.of(
                          "myInnerObject",
                          new ObjectSchema()
                              .properties(Map.of("myField", new Schema().type("boolean"))))));

      assertEquals(
          new Pair<>(
              new AstReference("p", "MyObject", List.of(), false),
              Set.of(
                  AstClass.of(
                      "p",
                      "MyObject",
                      List.of(
                          new AstField(
                              new AstReference("p.myobject", "MyInnerObject", List.of(), false),
                              "myInnerObject"))),
                  AstClass.of(
                      "p.myobject",
                      "MyInnerObject",
                      List.of(new AstField(astBoolean(), "myField"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the outer generated type but the AST for both the outer and"
              + " nested types");
    }

    @Test
    void evaluateWithReferenceProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyObject",
              new ObjectSchema()
                  .properties(Map.of("myField", new Schema().$ref("#/components/schemas/MyRef"))));

      assertEquals(
          new Pair<>(
              new AstReference("p", "MyObject", List.of(), false),
              Set.of(
                  AstClass.of(
                      "p",
                      "MyObject",
                      List.of(
                          new AstField(
                              new AstReference("p", "MyRef", List.of(), false), "myField"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the outer generated type and its AST, but no AST for the"
              + " referenced type, which must be evaluated separately");
    }

    @Test
    void evaluateWithInlineArrayProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyObject",
              new ObjectSchema()
                  .properties(
                      Map.of("myField", new ArraySchema().items(new Schema().type("boolean")))));

      assertEquals(
          new Pair<>(
              new AstReference("p", "MyObject", List.of(), false),
              Set.of(
                  AstClass.of(
                      "p", "MyObject", List.of(new AstField(astListOf(astBoolean()), "myField"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference to the generated type and its AST, which does not generate any"
              + " new type for the nested array");
    }

    @Test
    void evaluateWithMultipleProperties() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyObject",
              new ObjectSchema()
                  .properties(
                      Map.of(
                          "myField1", new Schema().type("boolean"),
                          "myField2", new Schema().$ref("#/components/schemas/MyRef"))));

      assertEquals(
          new Pair<>(
              new AstReference("p", "MyObject", List.of(), false),
              Set.of(
                  AstClass.of(
                      "p",
                      "MyObject",
                      List.of(
                          new AstField(astBoolean(), "myField1"),
                          new AstField(
                              new AstReference("p", "MyRef", List.of(), false), "myField2"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "The AST contains one field for each of multiple properties");
    }
  }

  @Nested
  class ArraySchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluateWithScalarItem(
        String oasType, String oasFormat, String javaPackage, String javaClass) {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyArray",
              new ArraySchema().items(new Schema().type(oasType).format(oasFormat)));

      assertEquals(
          new Pair<>(
              astListOf(new AstReference(javaPackage, javaClass, List.of(), true)), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the list, but no AST since no new types are generated");
    }

    @Test
    void evaluateWithInlineObjectItem() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyArray",
              new ArraySchema()
                  .items(
                      new ObjectSchema()
                          .properties(Map.of("myField", new Schema().type("boolean")))));

      assertEquals(
          new Pair<>(
              astListOf(new AstReference("p", "MyArrayItem", List.of(), false)),
              Set.of(
                  AstClass.of("p", "MyArrayItem", List.of(new AstField(astBoolean(), "myField"))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "defines the inline object in the current package with the -Item suffix in its class"
              + " name");
    }

    @Test
    void evaluateWithReferenceItem() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyArray",
              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));

      assertEquals(
          new Pair<>(astListOf(new AstReference("p", "MyRef", List.of(), false)), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the list, but no AST since no new types are generated (we"
              + " assume the reference is evaluated separately)");
    }

    @Test
    void evaluateWithArrayItem() {
      var actual =
          OasSchemaToAst.evaluate(
              "p",
              "MyArray",
              new ArraySchema().items(new ArraySchema().items(new Schema<>().type("boolean"))));

      assertEquals(
          new Pair<>(astListOf(astListOf(astBoolean())), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns a compose list AstReference and no AST since no new types are generated");
    }
  }
}
