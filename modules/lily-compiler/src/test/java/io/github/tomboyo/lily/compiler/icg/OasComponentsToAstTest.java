package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astListOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn2;
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

  @Nested
  class ScalarComponents {
    @ParameterizedTest
    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasComponentsToAstTest#scalarsSource")
    void evaluate(String oasType, String oasFormat, String javaPackage, String javaClass) {
      var actual =
          OasComponentsToAst.evaluate(
              "p", "MyComponent", new Schema().type(oasType).format(oasFormat));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn2.of("p", "MyComponent"),
                  new AstReference(javaPackage, javaClass, List.of(), true))),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Refs {
    @Test
    void evaluate() {
      var actual =
          OasComponentsToAst.evaluate(
              "p", "MyComponent", new Schema().$ref("#/components/schemas/MyRef"));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn2.of("p", "MyComponent"), new AstReference("p", "MyRef", List.of(), false))),
          actual.collect(Collectors.toSet()));
    }
  }

  @Nested
  class Arrays {
    @Test
    void evaluateWithScalarItem() {
      var actual =
          OasComponentsToAst.evaluate(
              "p", "MyComponent", new ArraySchema().items(new Schema<>().type("boolean")));

      assertEquals(
          Set.of(new AstClassAlias(Fqn2.of("p", "MyComponent"), astListOf(astBoolean()))),
          actual.collect(Collectors.toSet()),
          "Array components evaluate to aliases of lists");
    }

    @Test
    void evaluateWithObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              "p",
              "MyComponent",
              new ArraySchema()
                  .items(new ObjectSchema().properties(Map.of("myField", new ObjectSchema()))));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn2.of("p", "MyComponent"),
                  new AstReference(
                      "java.util",
                      "List",
                      List.of(
                          new AstReference("p.mycomponent", "MyComponentItem", List.of(), false)),
                      true)),
              AstClass.of(
                  Fqn2.of("p.mycomponent", "MyComponentItem"),
                  List.of(
                      new AstField(
                          new AstReference(
                              "p.mycomponent.mycomponentitem", "MyField", List.of(), false),
                          SimpleName.of("myField")))),
              AstClass.of(Fqn2.of("p.mycomponent.mycomponentitem", "MyField"), List.of())),
          actual.collect(Collectors.toSet()),
          "Inline types within aliases are defined in packages subordinate to the class alias");
    }

    @Test
    void evaluateWithArrayOfObjectItem() {
      var actual =
          OasComponentsToAst.evaluate(
              "p",
              "MyComponent",
              new ArraySchema().items(new ArraySchema().items(new ObjectSchema())));

      assertEquals(
          Set.of(
              AstClass.of(Fqn2.of("p.mycomponent", "MyComponentItem"), List.of()),
              new AstClassAlias(
                  Fqn2.of("p", "MyComponent"),
                  astListOf(
                      astListOf(
                          new AstReference(
                              "p.mycomponent", "MyComponentItem", List.of(), false))))),
          actual.collect(Collectors.toSet()),
          "item schemas in nested arrays evaluate to AstClasses nested beneath the alias and not"
              + " deeper");
    }

    @Test
    void evaluateWithRefItem() {
      var actual =
          OasComponentsToAst.evaluate(
              "p",
              "MyComponent",
              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));

      assertEquals(
          Set.of(
              new AstClassAlias(
                  Fqn2.of("p", "MyComponent"),
                  astListOf(new AstReference("p", "MyRef", List.of(), false)))),
          actual.collect(Collectors.toSet()),
          "arrays of refs evaluate to aliases of lists of the referent type");
    }
  }

  @Nested
  class Objects {
    @Test
    void evaluate() {
      assertEquals(
          OasSchemaToAst.evaluate("p", "MyComponent", new ObjectSchema())
              .right()
              .collect(Collectors.toSet()),
          OasComponentsToAst.evaluate("p", "MyComponent", new ObjectSchema())
              .collect(Collectors.toSet()),
          "Object components are evaluated the same as any other object schema (i.e. not aliased)");
    }
  }
}
