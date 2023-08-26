package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static io.github.tomboyo.lily.compiler.feature.components.schemas.TestSupport.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * These tests cover all components.schemas elements that are a $ref of some other schema. These schemas are generated
 * into "alias" types, which are wrappers around an instance of the referenced type.
 */
@Nested
@ExtendWith(LilyExtension.class)
public class RefsTests {

  @BeforeAll
  static void beforeAll(LilyExtension.LilyTestSupport support) {
    support.compileOas(
        """
                openapi: 3.0.2
                components:
                  schemas:
                    Test:
                      $ref: '#/components/schemas/Ref'
                    Ref:
                      properties:
                        foo:
                          type: string
                """);
  }

  @Test
  void testIsAlias(LilyExtension.LilyTestSupport support) {
    assertTrue(
        support.evaluate(
            """
                        var foo = new {{package}}.Ref("foo!");
                        return foo == new {{package}}.Test(foo).value();
                        """,
            Boolean.class),
        """
        `$ref` schemas are generated as "aliases" of the referenced type. The aliased value is accessible
        through the `value()` getter regardless of property name.
        """);
  }

  @Test
  void testSerialization(LilyExtension.LilyTestSupport support) throws Exception {
    var obj =
        support.evaluate(
            """
                        return new {{package}}.Test(new {{package}}.Ref("foo!"));
                        """);
    assertEquals(
        "{\"foo\":\"foo!\"}",
        MAPPER.writeValueAsString(obj),
        "Aliases are serialized as if they were only their aliased value");
  }

  @Test
  void testDeserialization(LilyExtension.LilyTestSupport support) throws Exception {
    assertEquals(
        support.evaluate(
            """
                      return new {{package}}.Test(new {{package}}.Ref("foo!"));
                      """),
        MAPPER.readValue("{\"foo\":\"foo!\"}", support.getClassForName("{{package}}.Test")));
  }
}
