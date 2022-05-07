package com.github.tomboyo.lily.http.encoding;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Encoding {

  private static final ObjectMapper simpleMapper =
      new ObjectMapper(new SimpleFactory())
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);
  private static final ObjectMapper simpleExplodeMapper =
      new ObjectMapper(new SimpleExplodeFactory())
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private Encoding() {}

  // TODO: this api
  public static String simple(Object o) throws JsonProcessingException {
    return simpleMapper.writer().writeValueAsString(o);
  }

  public static String simpleExplode(Object o) throws JsonProcessingException {
    return simpleExplodeMapper.writeValueAsString(o);
  }
}
