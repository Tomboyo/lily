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
                      - type: object
                        properties:
                          foo:
                            type: integer
                            format: int32
                      - type: string
                      #- type: string
                      - type: string
                        format: email
                      #- type: integer
                      #    format: int32
                  Foo:
                    type: string
            """);
    }

    // 1. There must be a MySchema interface
    // 2. The following implement the interface:
    //    - [x] com.exmaple.Foo
    //    - [x] com.example.MySchema1
    //    - [x] com.example.StringAlias
    //    - [x] c.e.m.StringEmailAlias
    //    - [ ] c.e.m.IntegerInt32Alias
    // 3. The type string entries should collapse into just one

    @Test
    void fooImplementsInterface(LilyTestSupport support) {
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
    void mySchema2ImplementsInterface(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
          return {{package}}.MySchema.class.isAssignableFrom(
              {{package}}.MySchema2.class);
          """,
              Boolean.class));
    }

    @Test
    void myStringAliasImplementsInterface(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              return {{package}}.MySchema.class.isAssignableFrom(
                  {{package}}.MySchemaStringAlias.class);
              """,
              Boolean.class));
    }

    @Test
    void myStringEmailAliasImplementsInterface(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
          return {{package}}.MySchema.class.isAssignableFrom(
            {{package}}.MySchemaStringEmailAlias.class);
          """,
              Boolean.class));
    }
  }
}
