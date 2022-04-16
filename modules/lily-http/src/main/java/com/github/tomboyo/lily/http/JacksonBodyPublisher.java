package com.github.tomboyo.lily.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;

public class JacksonBodyPublisher {
  public static <T> HttpRequest.BodyPublisher of(ObjectMapper objectMapper, T body) {
    try {
      return HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body));
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
