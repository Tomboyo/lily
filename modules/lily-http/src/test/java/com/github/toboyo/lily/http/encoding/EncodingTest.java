package com.github.toboyo.lily.http.encoding;

import static com.github.tomboyo.lily.http.encoding.Encoding.formExplode;
import static com.github.tomboyo.lily.http.encoding.Encoding.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EncodingTest {

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
        arguments("number,7", new Singleton(7)));
  }

  @ParameterizedTest
  @MethodSource("simpleSource")
  public void simpleTest(String expected, Object arg) throws JsonProcessingException {
    assertEquals(expected, simple(arg));
  }

  static Stream<Arguments> formExplodeSource() {
    /**
     * All form style parameters have to have keys, so the only valid arguments are KV stores like
     * Maps and Jackson-annotated objectes.
     */
    return Stream.of(
        /*
         * Primitives
         */
        arguments("?key=101", Map.of("key", BigInteger.valueOf(101))),
        arguments("?key=101", Map.of("key", 101L)),
        arguments("?key=1", Map.of("key", 1)),
        arguments("?key=10.1", Map.of("key", BigDecimal.valueOf(10.1))),
        arguments("?key=1.2", Map.of("key", 1.2d)),
        arguments("?key=1.2", Map.of("key", 1.2f)),
        arguments("?key=Foo", Map.of("key", "Foo")),
        // RFC3339 (ISO8601) full-date
        arguments("?key=2000-10-01", Map.of("key", LocalDate.of(2000, 10, 1))),
        // RFC3339 (ISO8601) date-time
        arguments(
            "?key=2000-10-01T06:30:25.00052Z",
            Map.of("key", OffsetDateTime.of(2000, 10, 1, 6, 30, 25, 520_000, ZoneOffset.UTC))),
        arguments("?key=false", Map.of("key", false)),
        /*
         * Arrays
         */
        arguments("?key=123&key=cats&key=22.34", Map.of("key", List.of(123, "cats", 22.34))),
        arguments("?key=123", Map.of("key", List.of(123))),
        /*
         * Objects
         */
        arguments("?number=5&text=Foo", new Multiton(5, "Foo")),
        arguments("?number=7", new Singleton(7)));
  }

  @ParameterizedTest
  @MethodSource("formExplodeSource")
  public void formExplodeTest(String expected, Object arg) throws JsonProcessingException {
    assertEquals(expected, formExplode(arg));
  }

  @JsonPropertyOrder({"number", "text"})
  private record Multiton(@JsonProperty("number") int number, @JsonProperty("text") String text) {}

  private record Singleton(@JsonProperty("number") int number) {}
}
