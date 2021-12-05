package com.github.tomboyo.lily.example.localspec;

import com.example.MyObjectType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerializationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.findAndRegisterModules();
  }

  @Test
  public void myObjectType() throws Exception {
    // Local zone is serialized as-is but deserialized to UTC, so use UTC to ensure they're equal.
    var data = new MyObjectType(false, ZonedDateTime.now(ZoneId.of("UTC")));

    assertEquals(
        data,
        MAPPER.readValue(MAPPER.writeValueAsString(data), MyObjectType.class));
  }
}
