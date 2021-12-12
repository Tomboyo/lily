package com.github.tomboyo.lily.itproject;

import com.example.MyObject;
import com.example.MyObject2;
import com.example.MySimpleAlias;
import com.example.myobject2.Foo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.github.tomboyo.lily.itproject.testsupport.Support.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerializationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER
        .registerModule(new JavaTimeModule())
        // Prefer ISO-like formats instead of arrays and floats
        .configure(WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Nested
  public class MyObjectTests {
    @Test
    public void toJson() throws Exception {
      assertJsonEquals(
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
                "m": "2021-01-01T00:00:00.012+01:00"
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
              new Byte[]{ (byte) 3},
              new Byte[]{ (byte) 7},
              LocalDate.of(2021, 1, 1),
              OffsetDateTime.parse("2021-01-01T00:00:00.012+01:00")
          ),
          MAPPER
      );
    }
  }

  @Nested
  public class MyObject2Tests {
    @Test
    public void toJson() throws Exception {
      assertJsonEquals("""
            {
              "foo": {
                "bar": "value"
              }
            }""",
          new MyObject2(
              new Foo("value")),
          MAPPER);
      }
  }

  @Nested
  public class MySimpleAliasTests {
    @Test
    public void toJson() throws Exception {
      assertJsonEquals(
          "\"value-string\"",
          new MySimpleAlias("value-string"),
          MAPPER);
    }

    @Test
    public void fromJson() throws Exception {
      assertEquals(
          MAPPER.readValue("\"value-string\"", MySimpleAlias.class),
          new MySimpleAlias("value-string")
      );
    }
  }
}
