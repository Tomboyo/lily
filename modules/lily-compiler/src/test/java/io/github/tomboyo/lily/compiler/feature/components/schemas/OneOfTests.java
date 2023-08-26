package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * These tests cover all components.schemas elements with the oneOf composed schema keyword. Composed schemas are
 * generated into sealed interfaces (Java's sum type).
 */
@Nested
@ExtendWith(LilyExtension.class)
public class OneOfTests {
  @BeforeAll
  static void beforeAll(LilyExtension.LilyTestSupport support) {
    support.compileOas(
        """
                openapi: 3.0.2
                components:
                    schemas:
                      MySchema:
                        oneOf:
                          - $ref: '#/components/schemas/Foo'
                          - type: object
                            properties:
                              foo:
                                type: integer
                                format: int32
                          - type: string
                          - type: string
                            format: email
                          - type: integer
                            format: int32
                      Foo:
                        type: string
                """);
  }

  @Test
  void fooImplementsInterface(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        return {{package}}.MySchema.class.isAssignableFrom(
                            {{package}}.Foo.class);
                        """,
            Boolean.class));
  }

  /*
   * The second element in the OneOf is anonymous, so we combine the root component's name with its position to create
   * the name MySchema2. This name is unstable between schema versions: if the position of the type in the list
   * changes, then so does the generated type name. This is unavoidable.
   *
   * Note that MySchema2 is in the same package as MySchema because MySchema is a sealed interface. Members of a
   * sealed interface either have to be in the same package or the same non-default module (this project isn't
   * modular).
   */
  @Test
  void mySchema2ImplementsInterface(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        return {{package}}.MySchema.class.isAssignableFrom(
                            {{package}}.MySchema2.class);
                        """,
            Boolean.class));
  }

  @Test
  void myStringAliasImplementsInterface(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        return {{package}}.MySchema.class.isAssignableFrom(
                            {{package}}.MySchemaStringAlias.class);
                        """,
            Boolean.class));
  }

  @Test
  void myStringEmailAliasImplementsInterface(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        return {{package}}.MySchema.class.isAssignableFrom(
                          {{package}}.MySchemaStringEmailAlias.class);
                        """,
            Boolean.class));
  }

  @Test
  void myIntegerInt32AliasImplementsInterface(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        return {{package}}.MySchema.class.isAssignableFrom(
                          {{package}}.MySchemaIntegerInt32Alias.class);
                        """,
            Boolean.class));
  }
}
