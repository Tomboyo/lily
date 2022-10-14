package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoders.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

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

public class EncodersTest {

  @Nested
  class Simple {

    static Stream<Arguments> simpleSource() {
      // arguments: expected, parameter name, object to encode
      return Stream.of(
          /*
           * Primitives
           */
          arguments("101", "key", BigInteger.valueOf(101)),
          arguments("101", "key", 101L),
          arguments("1", "key", 1),
          arguments("10.1", "key", BigDecimal.valueOf(10.1)),
          arguments("1.2", "key", 1.2d),
          arguments("1.2", "key", 1.2f),
          arguments("Foo", "key", "Foo"),
          // RFC3339 (ISO8601) full-date
          arguments("2000-10-01", "key", LocalDate.of(2000, 10, 1)),
          // RFC3339 (ISO8601) date-time
          arguments(
              "2000-10-01T06:30:25.00052Z",
              "key",
              OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC)),
          arguments("false", "key", false),
          // Null pointer
          arguments("", "key", null),
          /*
           * Arrays
           */
          arguments("123,cats,22.34", "keys", List.of(123, "cats", 22.34)),
          arguments("123", "keys", List.of(123)),
          // Null pointer
          arguments("x,,y", "keys", nullableList("x", null, "y")),
          /*
           * Objects
           */
          arguments("number,5,text,Foo", "keys", new Multiton(5, "Foo")),
          arguments("number,7", "keys", new Singleton(7)),
          arguments("x,,y,", "keys", nullableOrderedMap("x", null, "y", null)));
    }

    @ParameterizedTest
    @MethodSource("simpleSource")
    void test(String expected, String parameterName, Object obj) {
      assertEquals(expected, simple().encode(parameterName, obj));
    }

    @Test
    void nestedObjectsInObjects() {
      assertThrows(
          Exception.class,
          () -> simple().encode("keys", Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class,
          () -> simple().encode("keys", List.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedListsInObjects() {
      assertThrows(
          Exception.class,
          () -> simple().encode("keys", Map.of("foo", List.of("not", "supported"))));
    }

    @Test
    void nestedListsInLists() {
      assertThrows(
          Exception.class,
          () -> simple().encode("keys", List.of("foo", List.of("not", "supported"))));
    }
  }

  @Nested
  class FormExploded {
    static Stream<Arguments> parameters() {
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
    @MethodSource("parameters")
    void formExplodeTest(String expected, String name, Object obj) {
      assertEquals(expected, formExploded().encode(name, obj));
    }

    @Test
    void nestedObjectsInObjects() {
      assertThrows(
          Exception.class,
          () -> formExploded().encode("param", Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class,
          () -> formExploded().encode("param", List.of(Map.of("not", "supported"))));
    }

    @Test
    void nestedListsInLists() {
      assertThrows(
          Exception.class,
          () -> formExploded().encode("param", List.of(List.of("not", "supported"))));
    }

    @Test
    void nestedListsInObjects() {
      assertThrows(
          Exception.class,
          () -> formExploded().encode("param", Map.of("key", List.of("not", "supported"))));
    }
  }

  @Nested
  class FormContinuationExploded {
    static Stream<Arguments> parameters() {
      // arguments: expected encoding, parameter name, object to encode.
      return Stream.of(
          /*
           * Primitives
           */
          arguments("&key=101", "key", BigInteger.valueOf(101)),
          arguments("&key=101", "key", 101L),
          arguments("&key=1", "key", 1),
          arguments("&key=10.1", "key", BigDecimal.valueOf(10.1)),
          arguments("&key=1.2", "key", 1.2d),
          arguments("&key=1.2", "key", 1.2f),
          arguments("&key=Foo", "key", "Foo"),
          // RFC3339 (ISO8601) full-date
          arguments("&key=2000-10-01", "key", LocalDate.of(2000, 10, 1)),
          // RFC3339 (ISO8601) date-time
          arguments(
              "&key=2000-10-01T06%3A30%3A25.00052Z",
              "key", OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC)),
          arguments("&key=false", "key", false),
          // Reserved string
          arguments("&key%3F=%3F", "key?", "?"),
          /*
           * Arrays
           */
          arguments("&keys=123&keys=cats&keys=22.34", "keys", List.of(123, "cats", 22.34)),
          arguments("&keys=123", "keys", List.of(123)),
          // Null pointers
          arguments("&keys=&keys=", "keys", nullableList(null, null)),
          // Reserved string
          arguments("&keys%3F=%3F&keys%3F=%3F", "keys?", List.of("?", "?")),
          /*
           * Objects
           */
          arguments("&number=5&text=Foo", "keys", new Multiton(5, "Foo")),
          arguments("&number=7", "keys", new Singleton(7)),
          // Null pointers
          arguments("&foo=&bar=", "keys", nullableOrderedMap("foo", null, "bar", null)),
          // Reserved string
          arguments("&foo=%3F&bar%3F=%3F", "keys", nullableOrderedMap("foo", "?", "bar?", "?")));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void formContinuationExplodedTest(String expected, String name, Object obj) {
      assertEquals(expected, formContinuationExploded().encode(name, obj));
    }

    @Test
    void nestedObjectsInObjects() {
      assertThrows(
          Exception.class,
          () ->
              formContinuationExploded()
                  .encode("param", Map.of("foo", Map.of("not", "supported"))));
    }

    @Test
    void nestedObjectsInLists() {
      assertThrows(
          Exception.class,
          () -> formContinuationExploded().encode("param", List.of(Map.of("not", "supported"))));
    }

    @Test
    void nestedListsInLists() {
      assertThrows(
          Exception.class,
          () -> formContinuationExploded().encode("param", List.of(List.of("not", "supported"))));
    }

    @Test
    void nestedListsInObjects() {
      assertThrows(
          Exception.class,
          () ->
              formContinuationExploded()
                  .encode("param", Map.of("key", List.of("not", "supported"))));
    }
  }

  @Nested
  class FirstThenRestEncoder {
    @Test
    void alternatesStrategyAfterFirstCall() {
      var first = mock(Encoder.class);
      var rest = mock(Encoder.class);

      var subject = new Encoders.FirstThenRestEncoder(first, rest);

      subject.encode("key1", "value1");
      subject.encode("key2", "value2");
      subject.encode("key3", "value3");

      // The first parameter is encoded using the 'first' encoder.
      verify(first).encode(eq("key1"), eq("value1"));

      // The second and subsequent parameters are encoded usig the 'rest' encoder.
      verify(rest).encode(eq("key2"), eq("value2"));
      verify(rest).encode(eq("key3"), eq("value3"));
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
