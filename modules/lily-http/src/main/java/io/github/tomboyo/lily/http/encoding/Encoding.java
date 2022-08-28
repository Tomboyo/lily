package io.github.tomboyo.lily.http.encoding;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.UncheckedIOException;

public class Encoding {

  private static final ObjectMapper simpleMapper =
      new ObjectMapper(new SimpleFactory())
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private static final ObjectMapper formExplodeMapper =
      new ObjectMapper(new FormExplodeFactory())
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private Encoding() {}

  public static String simple(Object o) {
    try {
      return simpleMapper.writer().writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String formExplode(Object o) {
    try {
      return formExplodeMapper.writer().writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
