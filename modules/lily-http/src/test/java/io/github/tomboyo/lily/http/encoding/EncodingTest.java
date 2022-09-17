package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoding.formExplode;
import static io.github.tomboyo.lily.http.encoding.Encoding.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
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
          arguments("x,,y,", nullableMap("x", null, "y", null)));
    }

    @ParameterizedTest
    @MethodSource("simpleSource")
    void test(String expected, Object arg) throws JsonProcessingException {
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
  class FormExplode {
    static Stream<Arguments> formExplodeSource() {
      /**
       * All form style parameters have to have keys, so the only valid arguments are KV stores like
       * Maps and Jackson-annotated objectes.
       */
      return Stream.of(
          /*
           * Primitives
           */
          arguments("key=101", Map.of("key", BigInteger.valueOf(101))),
          arguments("key=101", Map.of("key", 101L)),
          arguments("key=1", Map.of("key", 1)),
          arguments("key=10.1", Map.of("key", BigDecimal.valueOf(10.1))),
          arguments("key=1.2", Map.of("key", 1.2d)),
          arguments("key=1.2", Map.of("key", 1.2f)),
          arguments("key=Foo", Map.of("key", "Foo")),
          // RFC3339 (ISO8601) full-date
          arguments("key=2000-10-01", Map.of("key", LocalDate.of(2000, 10, 1))),
          // RFC3339 (ISO8601) date-time
          arguments(
              "key=2000-10-01T06:30:25.00052Z",
              Map.of("key", OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC))),
          arguments("key=false", Map.of("key", false)),
          /*
           * Arrays
           */
          arguments("key=123&key=cats&key=22.34", Map.of("key", List.of(123, "cats", 22.34))),
          arguments("key=123", Map.of("key", List.of(123))),
          /*
           * Objects
           */
          arguments("number=5&text=Foo", new Multiton(5, "Foo")),
          arguments("number=7", new Singleton(7)),
          /*
           * Null pointers
           */
          arguments("foo=&bar=", nullableMap("foo", null, "bar", null)),
          arguments("key=&key=", Map.of("key", nullableList(null, null))));
    }

    @ParameterizedTest
    @MethodSource("formExplodeSource")
    void formExplodeTest(String expected, Object arg) throws JsonProcessingException {
      assertEquals(expected, formExplode(arg));
    }

    @Test
    void nonObjectParameter() {
      assertThrows(Exception.class, () -> Encoding.formExplode("not an object"));
    }

    @Test
    void nestedObjects() {
      assertThrows(
          Exception.class, () -> Encoding.formExplode(Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class,
          () -> Encoding.formExplode(Map.of("foo", List.of("bar", Map.of("not", "supported")))));
    }
  }

  private static List<Object> nullableList(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }

  private static LinkedHashMap<Object, Object> nullableMap(Object... objects) {
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
