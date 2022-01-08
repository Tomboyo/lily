package com.github.tomboyo.lily.itproject;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.MyInlineObjectArrayAlias;
import com.example.MyNumberArrayAlias;
import com.example.MyObject;
import com.example.MyObject2;
import com.example.MyRefArrayAlias;
import com.example.MySimpleAlias;
import com.example.myinlineobjectarrayalias.MyInlineObjectArrayAliasItem;
import com.example.myobject2.Foo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests that all generated sources serialize and deserialize to expected values. */
public class SerializationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER
        .registerModule(new JavaTimeModule())
        // Prefer ISO-like formats instead of arrays and floats
        .configure(WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @ParameterizedTest
  @MethodSource("parameterSource")
  public void toJson(TestParameter<?> params) throws Exception {
    var actualJson = MAPPER.readValue(MAPPER.writeValueAsString(params.asObject), JsonNode.class);
    var expectedJson = MAPPER.readValue(params.asJson, JsonNode.class);
    assertEquals(expectedJson, actualJson);
  }

  @ParameterizedTest
  @MethodSource("parameterSource")
  public <T> void fromJson(TestParameter<T> params) throws Exception {
    params.assertObjectEquals.accept(params.asObject, MAPPER.readValue(params.asJson, params.type));
  }

  private record TestParameter<T>(
      String asJson, T asObject, Class<T> type, BiConsumer<T, T> assertObjectEquals) {
    @SuppressWarnings("unchecked")
    public TestParameter(String asJson, T asObject, BiConsumer<T, T> assertObjectEquals) {
      this(asJson, asObject, (Class<T>) asObject.getClass(), assertObjectEquals);
    }

    public TestParameter(String asJson, T asObject) {
      this(asJson, asObject, Assertions::assertEquals);
    }
  }

  public static Stream<Arguments> parameterSource() {
    return Stream.of(
            myObjectTestParameter(),
            myObject2TestParameter(),
            mySimpleAliasTestParameter(),
            myRefArrayAliasTestParameter(),
            myNumberArrayAliasTestParameter(),
            myInlineObjectArrayAliasTestParameter())
        .map(x -> arguments(Named.of(x.type.getSimpleName(), x)));
  }

  private static TestParameter<MyObject> myObjectTestParameter() {
    return new TestParameter<>(
        """
              {
                "a": true,
                "b": 0,
                "c": 1,
                "d": 2,
                "e": 3,
                "f": 4.1,
                "g": 5.2,
                "h": "foo",
                "i": "password",
                "j": [3],
                "k": [7],
                "l": "2021-01-01",
                "m": "2021-01-01T00:00:00.012Z"
              }
              """,
        new MyObject(
            true,
            BigInteger.ZERO,
            1,
            2L,
            BigDecimal.valueOf(3L),
            4.1d,
            5.2f,
            "foo",
            "password",
            new Byte[] {(byte) 3},
            new Byte[] {(byte) 7},
            LocalDate.of(2021, 1, 1),
            OffsetDateTime.parse("2021-01" + "-01T00:00:00.012Z")),
        (expected, actual) -> {
          assertEquals(
              List.of(
                  expected.a(),
                  expected.b(),
                  expected.c(),
                  expected.d(),
                  expected.e(),
                  expected.f(),
                  expected.g(),
                  expected.h(),
                  expected.i(),
                  expected.l(),
                  expected.m()),
              List.of(
                  actual.a(),
                  actual.b(),
                  actual.c(),
                  actual.d(),
                  actual.e(),
                  actual.f(),
                  actual.g(),
                  actual.h(),
                  actual.i(),
                  actual.l(),
                  actual.m()));
          assertArrayEquals(expected.j(), actual.j());
          assertArrayEquals(expected.k(), actual.k());
        });
  }

  private static TestParameter<MyObject2> myObject2TestParameter() {
    return new TestParameter<>(
        "{ \"foo\": { \"bar\": \"value\" } }", new MyObject2(new Foo("value")));
  }

  private static TestParameter<MySimpleAlias> mySimpleAliasTestParameter() {
    return new TestParameter<>("\"value\"", new MySimpleAlias("value"));
  }

  private static TestParameter<MyRefArrayAlias> myRefArrayAliasTestParameter() {
    return new TestParameter<>(
        "[{ \"foo\": { \"bar\": \"value\" } }]",
        new MyRefArrayAlias(List.of(new MyObject2(new Foo("value")))));
  }

  private static TestParameter<MyNumberArrayAlias> myNumberArrayAliasTestParameter() {
    return new TestParameter<>("[123]", new MyNumberArrayAlias(List.of(BigDecimal.valueOf(123))));
  }

  private static TestParameter<MyInlineObjectArrayAlias> myInlineObjectArrayAliasTestParameter() {
    return new TestParameter<>(
        "[{\"foo\": \"foo!\"}]",
        new MyInlineObjectArrayAlias(List.of(new MyInlineObjectArrayAliasItem("foo!"))));
  }
}
