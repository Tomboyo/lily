package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TestSupport {
  static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER
        .registerModule(new JavaTimeModule())
        // Prefer ISO-like formats instead of arrays and floats
        .configure(WRITE_DATES_AS_TIMESTAMPS, false);
  }
}
