package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.ast.AstReference.newTypeRef;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBigDecimal;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBigInteger;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astByteArray;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astDouble;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astFloat;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astInteger;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astListOf;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astLocalDate;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astLong;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astOffsetDateTime;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astString;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn2;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
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
        arguments("boolean", null, astBoolean()),
        arguments("boolean", "unsupported-format", astBoolean()),
        arguments("integer", null, astBigInteger()),
        arguments("integer", "unsupported-format", astBigInteger()),
        arguments("integer", "int32", astInteger()),
        arguments("integer", "int64", astLong()),
        arguments("number", null, astBigDecimal()),
        arguments("number", "unsupported-format", astBigDecimal()),
        arguments("number", "double", astDouble()),
        arguments("number", "float", astFloat()),
        arguments("string", null, astString()),
        arguments("string", "unsupportedFormat", astString()),
        arguments("string", "password", astString()),
        arguments("string", "byte", astByteArray()),
        arguments("string", "binary", astByteArray()),
        arguments("string", "date", astLocalDate()),
        arguments("string", "date-time", astOffsetDateTime()));
  }

  @Test
  public void unsupportedSchemaTypes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OasSchemaToAst.evaluate(
                PackageName.of("p"),
                SimpleName.of("MyBadSchema"),
                new Schema<>().type("unsupported-type")),
        "Unsupported types trigger runtime exceptions.");
  }

  @Nested
  class ScalarSchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluate(String oasType, String oasFormat, AstReference expectedRef) {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("fieldName"),
              new Schema().type(oasType).format(oasFormat));

      assertEquals(
          new Pair<>(expectedRef, Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns a standard type reference and no AST since nothing was generated");
    }
  }

  @Nested
  class ObjectSchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluateWithScalarProperty(String oasType, String oasFormat, AstReference expectedRef) {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyObject"),
              new ObjectSchema()
                  .properties(Map.of("myField", new Schema().type(oasType).format(oasFormat))));

      assertEquals(
          new Pair<>(
              newTypeRef(Fqn2.of("p", "MyObject"), List.of()),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyObject"),
                      List.of(new AstField(expectedRef, SimpleName.of("myField")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the generated type and its AST");
    }

    @Test
    void evaluateWithInlineObjectProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyObject"),
              new ObjectSchema()
                  .properties(
                      Map.of(
                          "myInnerObject",
                          new ObjectSchema()
                              .properties(Map.of("myField", new Schema().type("boolean"))))));

      assertEquals(
          new Pair<>(
              newTypeRef(Fqn2.of("p", "MyObject"), List.of()),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyObject"),
                      List.of(
                          new AstField(
                              newTypeRef(Fqn2.of("p.myobject", "MyInnerObject"), List.of()),
                              SimpleName.of("myInnerObject")))),
                  AstClass.of(
                      Fqn2.of("p.myobject", "MyInnerObject"),
                      List.of(new AstField(astBoolean(), SimpleName.of("myField")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the outer generated type but the AST for both the outer and"
              + " nested types");
    }

    @Test
    void evaluateWithReferenceProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyObject"),
              new ObjectSchema()
                  .properties(Map.of("myField", new Schema().$ref("#/components/schemas/MyRef"))));

      assertEquals(
          new Pair<>(
              newTypeRef(Fqn2.of("p", "MyObject"), List.of()),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyObject"),
                      List.of(
                          new AstField(
                              newTypeRef(Fqn2.of("p", "MyRef"), List.of()),
                              SimpleName.of("myField")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the outer generated type and its AST, but no AST for the"
              + " referenced type, which must be evaluated separately");
    }

    @Test
    void evaluateWithInlineArrayProperty() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyObject"),
              new ObjectSchema()
                  .properties(
                      Map.of("myField", new ArraySchema().items(new Schema().type("boolean")))));

      assertEquals(
          new Pair<>(
              newTypeRef(Fqn2.of("p", "MyObject"), List.of()),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyObject"),
                      List.of(new AstField(astListOf(astBoolean()), SimpleName.of("myField")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference to the generated type and its AST, which does not generate any"
              + " new type for the nested array");
    }

    @Test
    void evaluateWithMultipleProperties() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyObject"),
              new ObjectSchema()
                  .properties(
                      Map.of(
                          "myField1", new Schema().type("boolean"),
                          "myField2", new Schema().$ref("#/components/schemas/MyRef"))));

      assertEquals(
          new Pair<>(
              newTypeRef(Fqn2.of("p", "MyObject"), List.of()),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyObject"),
                      List.of(
                          new AstField(astBoolean(), SimpleName.of("myField1")),
                          new AstField(
                              newTypeRef(Fqn2.of("p", "MyRef"), List.of()),
                              SimpleName.of("myField2")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "The AST contains one field for each of multiple properties");
    }
  }

  @Nested
  class ArraySchemas {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
    void evaluateWithScalarItem(String oasType, String oasFormat, AstReference expectedRef) {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyArray"),
              new ArraySchema().items(new Schema().type(oasType).format(oasFormat)));

      assertEquals(
          new Pair<>(astListOf(expectedRef), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the list, but no AST since no new types are generated");
    }

    @Test
    void evaluateWithInlineObjectItem() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyArray"),
              new ArraySchema()
                  .items(
                      new ObjectSchema()
                          .properties(Map.of("myField", new Schema().type("boolean")))));

      assertEquals(
          new Pair<>(
              astListOf(newTypeRef(Fqn2.of("p", "MyArrayItem"), List.of())),
              Set.of(
                  AstClass.of(
                      Fqn2.of("p", "MyArrayItem"),
                      List.of(new AstField(astBoolean(), SimpleName.of("myField")))))),
          actual.mapRight(stream -> stream.collect(toSet())),
          "defines the inline object in the current package with the -Item suffix in its class"
              + " name");
    }

    @Test
    void evaluateWithReferenceItem() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyArray"),
              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));

      assertEquals(
          new Pair<>(astListOf(newTypeRef(Fqn2.of("p", "MyRef"), List.of())), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns an AstReference for the list, but no AST since no new types are generated (we"
              + " assume the reference is evaluated separately)");
    }

    @Test
    void evaluateWithArrayItem() {
      var actual =
          OasSchemaToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyArray"),
              new ArraySchema().items(new ArraySchema().items(new Schema<>().type("boolean"))));

      assertEquals(
          new Pair<>(astListOf(astListOf(astBoolean())), Set.of()),
          actual.mapRight(stream -> stream.collect(toSet())),
          "returns a compose list AstReference and no AST since no new types are generated");
    }
  }
}
