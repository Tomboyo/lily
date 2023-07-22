package io.github.tomboyo.lily.compiler.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public class ComposedSchemaTest {
  @Nested
  @ExtendWith(LilyExtension.class)
  class OneOf {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
            openapi: 3.0.2
            components:
                schemas:
                  MySchema:
                      oneOf:
                      - $ref: '#/components/schemas/Foo'
                      #- type: object
                      #    properties:
                      #    foo:
                      #        type: integer
                      #        format: int32
                      #- type: string
                      #- type: string
                      #- type: string
                      #    format: email
                      #- type: integer
                      #    format: int32
                  Foo:
                    type: string
            """);
    }

    @Test
    void test(LilyTestSupport support) {
      // 1. There must be a MySchema interface
      // 2. The following implement the interface:
      //    - com.exmaple.Foo
      //    - com.example.myschema.MySchema1
      //    - com.example.myschema.StringAlias
      //    - c.e.m.EmailAlias
      //    - c.e.m.Int32Alias
      // 3. The type string entries should collapse into just one
      assertTrue(
          support.evaluate(
              """
                    return {{package}}.MySchema.class.isAssignableFrom(
                        {{package}}.Foo.class);
                    """,
              Boolean.class));
    }
  }
}
