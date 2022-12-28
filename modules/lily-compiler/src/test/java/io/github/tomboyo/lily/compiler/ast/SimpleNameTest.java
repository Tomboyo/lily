package io.github.tomboyo.lily.compiler.ast;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SimpleNameTest {

  @Nested
  class KebabCaseInput {

    static Stream<Arguments> kebabCaseParameterSource() {
      // input, upperCamelCase, lowerCamelCase
      // arguments are grouped into equivalence classes where the inputs produce the same upper and
      // lower camel case output.
      return Stream.of(
          arguments("cat-dog", "CatDog", "catDog"),
          arguments("Cat-dog", "CatDog", "catDog"),
          arguments("CAT-dog", "CatDog", "catDog"),
          arguments("cat-Dog", "CatDog", "catDog"),
          arguments("cat-DOG", "CatDog", "catDog"),
          arguments("cat123-dog", "Cat123Dog", "cat123Dog"),
          arguments("Cat123-dog", "Cat123Dog", "cat123Dog"),
          arguments("CAT123-dog", "Cat123Dog", "cat123Dog"),
          arguments("cat-dog123", "CatDog123", "catDog123"),
          arguments("cat-Dog123", "CatDog123", "catDog123"),
          arguments("cat-Dog123", "CatDog123", "catDog123"),
          arguments("c", "C", "c"),
          arguments("c-a-t", "CAT", "cAT"),
          arguments("c-123", "C123", "c123"),
          arguments("c-123-at", "C123At", "c123At"));
    }

    @ParameterizedTest
    @MethodSource("kebabCaseParameterSource")
    void upperCamelCase(String input, String expected) {
      assertEquals(expected, SimpleName.of(input).upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("kebabCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(expected, SimpleName.of(input).lowerCamelCase());
    }
  }

  @Nested
  class SnakeCaseInput {

    static Stream<Arguments> snakeCaseParameterSource() {
      // input, upperCamelCase, lowerCamelCase
      // arguments are grouped into equivalence classes where the inputs produce the same upper and
      // lower camel case output.
      return Stream.of(
          arguments("cat_dog", "CatDog", "catDog"),
          arguments("Cat_dog", "CatDog", "catDog"),
          arguments("CAT_dog", "CatDog", "catDog"),
          arguments("cat_Dog", "CatDog", "catDog"),
          arguments("cat_DOG", "CatDog", "catDog"),
          arguments("cat123_dog", "Cat123Dog", "cat123Dog"),
          arguments("Cat123_dog", "Cat123Dog", "cat123Dog"),
          arguments("CAT123_dog", "Cat123Dog", "cat123Dog"),
          arguments("cat_dog123", "CatDog123", "catDog123"),
          arguments("cat_Dog123", "CatDog123", "catDog123"),
          arguments("cat_Dog123", "CatDog123", "catDog123"),
          arguments("c", "C", "c"),
          arguments("c_a_t", "CAT", "cAT"),
          arguments("c_123", "C123", "c123"),
          arguments("c_123_at", "C123At", "c123At"));
    }

    @ParameterizedTest
    @MethodSource("snakeCaseParameterSource")
    void upperCamelCase(String input, String expected) {
      assertEquals(expected, SimpleName.of(input).upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("snakeCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(expected, SimpleName.of(input).lowerCamelCase());
    }
  }

  @Nested
  class CamelCaseInput {

    static Stream<Arguments> camelCaseParameterSource() {
      // input, upperCamelCase, lowerCamelCase
      // arguments are grouped into equivalence classes where the inputs produce the same upper and
      // lower camel case output.
      return Stream.of(
          arguments("catDog", "CatDog", "catDog"),
          arguments("CatDog", "CatDog", "catDog"),
          arguments("CATDog", "CatDog", "catDog"),
          arguments("CatDOG", "CatDog", "catDog"),
          arguments("cat123", "Cat123", "cat123"),
          arguments("Cat123", "Cat123", "cat123"),
          arguments("CAT123", "Cat123", "cat123"),
          arguments("cA", "CA", "cA"),
          arguments("CA", "Ca", "ca"),
          arguments("catDog123", "CatDog123", "catDog123"),
          arguments("CatDog123", "CatDog123", "catDog123"),
          arguments("CATDog123", "CatDog123", "catDog123"),
          arguments("catDOG123", "CatDog123", "catDog123"),
          arguments("CatDOG123", "CatDog123", "catDog123"),
          arguments("catD123", "CatD123", "catD123"),
          arguments("CatD123", "CatD123", "catD123"),
          arguments("cat123dog", "Cat123Dog", "cat123Dog"),
          arguments("Cat123dog", "Cat123Dog", "cat123Dog"),
          arguments("CAT123dog", "Cat123Dog", "cat123Dog"),
          arguments("cat123Dog", "Cat123Dog", "cat123Dog"),
          arguments("Cat123Dog", "Cat123Dog", "cat123Dog"),
          arguments("CAT123Dog", "Cat123Dog", "cat123Dog"),
          arguments("cat123DOG", "Cat123Dog", "cat123Dog"),
          arguments("Cat123DOG", "Cat123Dog", "cat123Dog"),
          arguments("CAT123DOG", "Cat123Dog", "cat123Dog"));
    }

    @ParameterizedTest
    @MethodSource("camelCaseParameterSource")
    void upperCamelCase(String input, String expected) {
      assertEquals(expected, SimpleName.of(input).upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("camelCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(expected, SimpleName.of(input).lowerCamelCase());
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"123Name", "123-name", "123_name"}) // leading digits
  void illegalNameInputs(String input) {
    assertThrows(Exception.class, () -> SimpleName.of(input));
  }

  @Nested
  class Append {
    private static Stream<Arguments> allCasesParameterSource() {
      return Stream.of(
              CamelCaseInput.camelCaseParameterSource(),
              KebabCaseInput.kebabCaseParameterSource(),
              SnakeCaseInput.snakeCaseParameterSource())
          .flatMap(Function.identity());
    }

    @ParameterizedTest
    @MethodSource("allCasesParameterSource")
    void testAppend(String input, String upperCase) {
      assertEquals("FooBar" + upperCase, SimpleName.of("foo-bar").resolve(input).upperCamelCase());
    }
  }

  @Test
  void resolve() {
    assertEquals(SimpleName.of("GetFoo200"), SimpleName.of("Get").resolve("Foo").resolve("200"));
  }
}
