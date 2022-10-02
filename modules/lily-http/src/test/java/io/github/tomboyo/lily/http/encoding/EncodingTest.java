package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoding.Modifiers.EXPLODE;
import static io.github.tomboyo.lily.http.encoding.Encoding.form;
import static io.github.tomboyo.lily.http.encoding.Encoding.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EncodingTest {

  @Nested
  class Simple {

    static Stream<Arguments> simpleSource() {
      return Stream.of(
          /*
           * Primitives
           */
          arguments("101", BigInteger.valueOf(101)),
          arguments("101", 101L),
          arguments("1", 1),
          arguments("10.1", BigDecimal.valueOf(10.1)),
          arguments("1.2", 1.2d),
          arguments("1.2", 1.2f),
          arguments("Foo", "Foo"),
          // RFC3339 (ISO8601) full-date
          arguments("2000-10-01", LocalDate.of(2000, 10, 1)),
          // RFC3339 (ISO8601) date-time
          arguments(
              "2000-10-01T06:30:25.00052Z",
              OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC)),
          arguments("false", false),
          /*
           * Arrays
           */
          arguments("123,cats,22.34", List.of(123, "cats", 22.34)),
          arguments("123", List.of(123)),
          /*
           * Objects
           */
          arguments("number,5,text,Foo", new Multiton(5, "Foo")),
          arguments("number,7", new Singleton(7)),
          /*
           * Null pointers
           */
          arguments("", null),
          arguments("x,,y", nullableList("x", null, "y")),
          arguments("x,,y,", nullableOrderedMap("x", null, "y", null)));
    }

    @ParameterizedTest
    @MethodSource("simpleSource")
    void test(String expected, Object arg) {
      assertEquals(expected, simple(arg));
    }

    @Test
    void nestedObjectsInObjects() {
      assertThrows(
          Exception.class, () -> Encoding.simple(Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class, () -> Encoding.simple(List.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedListsInObjects() {
      assertThrows(
          Exception.class, () -> Encoding.simple(Map.of("foo", List.of("not", "supported"))));
    }

    @Test
    void nestedListsInLists() {
      assertThrows(
          Exception.class, () -> Encoding.simple(List.of("foo", List.of("not", "supported"))));
    }
  }

  @Nested
  class Form {
    static Stream<Arguments> formStyleQueryExpansionParameters() {
      // arguments: expected encoding, parameter name, object to encode.
      return Stream.of(
          /*
           * Primitives
           */
          arguments("?key=101", "key", BigInteger.valueOf(101)),
          arguments("?key=101", "key", 101L),
          arguments("?key=1", "key", 1),
          arguments("?key=10.1", "key", BigDecimal.valueOf(10.1)),
          arguments("?key=1.2", "key", 1.2d),
          arguments("?key=1.2", "key", 1.2f),
          arguments("?key=Foo", "key", "Foo"),
          // RFC3339 (ISO8601) full-date
          arguments("?key=2000-10-01", "key", LocalDate.of(2000, 10, 1)),
          // RFC3339 (ISO8601) date-time
          arguments(
              "?key=2000-10-01T06%3A30%3A25.00052Z",
              "key", OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC)),
          arguments("?key=false", "key", false),
          // Reserved string
          arguments("?key%3F=%3F", "key?", "?"),
          /*
           * Arrays
           */
          arguments("?keys=123&keys=cats&keys=22.34", "keys", List.of(123, "cats", 22.34)),
          arguments("?keys=123", "keys", List.of(123)),
          // Null pointers
          arguments("?keys=&keys=", "keys", nullableList(null, null)),
          // Reserved string
          arguments("?keys%3F=%3F&keys%3F=%3F", "keys?", List.of("?", "?")),
          /*
           * Objects
           */
          arguments("?number=5&text=Foo", "keys", new Multiton(5, "Foo")),
          arguments("?number=7", "keys", new Singleton(7)),
          // Null pointers
          arguments("?foo=&bar=", "keys", nullableOrderedMap("foo", null, "bar", null)),
          // Reserved string
          arguments("?foo=%3F&bar%3F=%3F", "keys", nullableOrderedMap("foo", "?", "bar?", "?")));
    }

    @ParameterizedTest
    @MethodSource("formStyleQueryExpansionParameters")
    void formExplodeTest(String expected, String name, Object obj) {
      assertEquals(expected, form(EXPLODE).apply(name, obj));
    }

    @Test
    void nestedObjectsInObjects() {
      assertThrows(
          Exception.class,
          () -> form(EXPLODE).apply("param", Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class, () -> form(EXPLODE).apply("param", List.of(Map.of("not", "supported"))));
    }

    @Test
    void nestedListsInLists() {
      assertThrows(
          Exception.class,
          () -> form(EXPLODE).apply("param", List.of(List.of("not", "supported"))));
    }

    @Test
    void nestedListsInObjects() {
      assertThrows(
          Exception.class,
          () -> form(EXPLODE).apply("param", Map.of("key", List.of("not", "supported"))));
    }
  }

  private static List<Object> nullableList(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }

  /** Returns a LinkedHashMap which allows null arguments and preserves insertion order. */
  private static LinkedHashMap<Object, Object> nullableOrderedMap(Object... objects) {
    var map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < objects.length; i += 2) {
      map.put(objects[i], objects[i + 1]);
    }
    return map;
  }

  @JsonPropertyOrder({"number", "text"})
  private record Multiton(@JsonProperty("number") int number, @JsonProperty("text") String text) {}

  private record Singleton(@JsonProperty("number") int number) {}
}
