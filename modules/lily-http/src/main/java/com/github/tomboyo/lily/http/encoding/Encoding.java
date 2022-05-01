package com.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Encoding {

  private static final ObjectMapper simpleMapper = new ObjectMapper(new SimpleFactory());
  private static final ObjectMapper simpleExplodeMapper =
      new ObjectMapper(new SimpleExplodeFactory());

  private Encoding() {}

  // TODO: this api
  public static String simple(Object o) throws JsonProcessingException {
    return simpleMapper.writer().writeValueAsString(o);
  }

  public static String simpleExplode(Object o) throws JsonProcessingException {
    return simpleExplodeMapper.writeValueAsString(o);
  }
}
