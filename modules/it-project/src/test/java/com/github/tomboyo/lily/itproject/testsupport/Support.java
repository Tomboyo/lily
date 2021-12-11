package com.github.tomboyo.lily.itproject.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Support {
  public static void assertJsonEquals(String expected, Object actual, ObjectMapper mapper)
      throws JsonProcessingException {
    var actualJson = mapper.readValue(mapper.writeValueAsString(actual), JsonNode.class);
    var expectedJson = mapper.readValue(expected, JsonNode.class);
    assertEquals(expectedJson, actualJson);
  }
}
