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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OasComponentsToAstTest {

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

  @Nested
  class ScalarComponents {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasComponentsToAstTest#scalarsSource")
    void evaluate(String oasType, String oasFormat, AstReference expectedRef) {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new Schema().type(oasType).format(oasFormat));

      assertEquals(
          Set.of(new AstClassAlias(Fqn.of("p", "MyComponent"), expectedRef)),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Refs {
    @Test
    void evaluate() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new Schema().$ref("#/components/schemas/MyRef"));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.of("p", "MyComponent"), newTypeRef(Fqn.of("p", "MyRef"), List.of()))),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Arrays {
    @Test
    void evaluateWithScalarItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new Schema<>().type("boolean")));

      assertEquals(
          Set.of(new AstClassAlias(Fqn.of("p", "MyComponent"), astListOf(astBoolean()))),
          actual.collect(Collectors.toSet()),
          "Array components evaluate to aliases of lists");
    }

    @Test
    void evaluateWithObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema()
                  .items(new ObjectSchema().properties(Map.of("myField", new ObjectSchema()))));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.of("p", "MyComponent"),
                  new AstReference(
                      Fqn.of("java.util", "List"),
                      List.of(newTypeRef(Fqn.of("p.mycomponent", "MyComponentItem"), List.of())),
                      false)),
              AstClass.of(
                  Fqn.of("p.mycomponent", "MyComponentItem"),
                  List.of(
                      new AstField(
                          newTypeRef(Fqn.of("p.mycomponent.mycomponentitem", "MyField"), List.of()),
                          SimpleName.of("myField")))),
              AstClass.of(Fqn.of("p.mycomponent.mycomponentitem", "MyField"), List.of())),
          actual.collect(Collectors.toSet()),
          "Inline types within aliases are defined in packages subordinate to the class alias");
    }

    @Test
    void evaluateWithArrayOfObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new ArraySchema().items(new ObjectSchema())));

      assertEquals(
          Set.of(
              AstClass.of(Fqn.of("p.mycomponent", "MyComponentItem"), List.of()),
              new AstClassAlias(
                  Fqn.of("p", "MyComponent"),
                  astListOf(
                      astListOf(
                          newTypeRef(Fqn.of("p.mycomponent", "MyComponentItem"), List.of()))))),
          actual.collect(Collectors.toSet()),
          "item schemas in nested arrays evaluate to AstClasses nested beneath the alias and not"
              + " deeper");
    }

    @Test
    void evaluateWithRefItem() {
      var actual =
          OasComponentsToAst.evaluate(
              PackageName.of("p"),
              SimpleName.of("MyComponent"),
              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn.of("p", "MyComponent"),
                  astListOf(newTypeRef(Fqn.of("p", "MyRef"), List.of())))),
          actual.collect(Collectors.toSet()),
          "arrays of refs evaluate to aliases of lists of the referent type");
    }
  }

  @Nested
  class Objects {
    @Test
    void evaluate() {
      assertEquals(
          OasSchemaToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .right()
              .collect(Collectors.toSet()),
          OasComponentsToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .collect(Collectors.toSet()),
          "Object components are evaluated the same as any other object schema (i.e. not aliased)");
    }
  }
}
