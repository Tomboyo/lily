package io.github.tomboyo.lily.compiler.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class Fqn2Test {

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
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("kebabCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().lowerCamelCase());
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
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("snakeCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().lowerCamelCase());
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
          arguments("cat123dog", "Cat123dog", "cat123dog"),
          arguments("Cat123dog", "Cat123dog", "cat123dog"),
          arguments("CAT123dog", "Cat123dog", "cat123dog"),
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
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().upperCamelCase());
    }

    @ParameterizedTest
    @MethodSource("camelCaseParameterSource")
    void lowerCamelCase(String input, String unused, String expected) {
      assertEquals(
          expected, Fqn2.newBuilder().withClassName(input).build().className().lowerCamelCase());
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"123Name", "123-name", "123_name"}) // leading digits
  void illegalNameInputs(String input) {
    assertThrows(Exception.class, () -> Fqn2.newBuilder().withClassName(input).build());
  }

  @Nested
  class FullyQualifiedName {
    private static final Stream<Arguments> fullyQualifiedParameterSource() {
      return Stream.of(
          arguments("com", new String[] {"example"}),
          arguments(".com.", new String[] {".example."}),
          arguments("com.example", new String[] {}),
          arguments("", new String[] {"com.example"}),
          arguments("com.example", new String[] {}));
    }

    @ParameterizedTest
    @MethodSource("fullyQualifiedParameterSource")
    void fullyQualifiedName(String first, String... rest) {
      assertEquals(
          "com.example.FooBarBaz",
          Fqn2.newBuilder()
              .withPackage(first, rest)
              .withClassName("FooBarBaz")
              .build()
              .fullyQualifiedName());
    }

    @Test
    void nullFirstPackagePart() {
      assertThrows(
          NullPointerException.class,
          () -> Fqn2.newBuilder().withPackage((String) null).withClassName("Foo").build());
    }

    @Test
    void nullRestPackageParts() {
      assertThrows(
          NullPointerException.class,
          () -> Fqn2.newBuilder().withPackage("", (String[]) null).withClassName("Foo").build());
    }
  }

  @Nested
  class AsPath {
    @Test
    void usingOf() {
      assertEquals(
          Path.of("io/github/tomboyo/lily/example/Test.java"),
          Fqn2.of("io.github.tomboyo.lily.example", "Test").asPath());
    }

    @Test
    void usingBuilder() {
      assertEquals(
          Path.of("io/github/tomboyo/lily/example/Test.java"),
          Fqn2.newBuilder()
              .withPackage("io.github", "tomboyo", "lily.example")
              .withClassName("Test")
              .build()
              .asPath());
    }
  }
}
