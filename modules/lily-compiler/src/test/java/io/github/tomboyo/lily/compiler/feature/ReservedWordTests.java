package io.github.tomboyo.lily.compiler.feature;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * These tests verify that reserved words can be used as parameter names, such that e.g. a parameter
 * called "super" does not cause a compilation error.
 */
public class ReservedWordTests {

  @Retention(RUNTIME)
  @Target(METHOD)
  @CsvSource({
    "abstract",
    "continue",
    "for",
    "new",
    "switch",
    "assert",
    "default",
    "goto",
    "package",
    "synchronized",
    "boolean",
    "do",
    "if",
    "private",
    "this",
    "break",
    "double",
    "implements",
    "protected",
    "throw",
    "byte",
    "else",
    "import",
    "public",
    "throws",
    "case",
    "enum",
    "instanceof",
    "return",
    "transient",
    "catch",
    "extends",
    "int",
    "short",
    "try",
    "char",
    "final",
    "interface",
    "static",
    "void",
    "class",
    "finally",
    "long",
    "strictfp",
    "volatile",
    "const",
    "float",
    "native",
    "super",
    "while",
    "true",
    "false",
    "null"
  })
  @interface ReservedWordsSource {}

  // TODO: parameterize this with all reserved keywords
  // TODO: consider renaming this? It should concentrate on the schema, not the impl
  @Nested
  class AstClassTest {
    @ParameterizedTest
    @ReservedWordsSource
    @ExtendWith(LilyExtension.class)
    void test(String reservedWord, LilyTestSupport support) {
      support.compileOas(
          """
                    openapi: 3.0.2
                    components:
                      schemas:
                        MyComponent:
                          properties:
                            %s:
                              type: string
                    """
              .formatted(reservedWord));

      assertTrue(
          support.evaluate(
              """
                        var x = "foo";
                        return x == new {{package}}.MyComponent("foo").get_{{Name}}().orElseThrow();
                        """,
              Boolean.class,
              "Name",
              reservedWord.substring(0, 1).toUpperCase() + reservedWord.substring(1)),
          "When reserved words are used as OAS parameter names, they are escaped in the generated"
              + " code");
    }
  }
}
