package com.github.tomboyo.lily.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;

/** Deserialize a response body with a Jackson {@Code ObjectMapper}. */
public class JacksonBodyHandler<T> implements HttpResponse.BodyHandler<T> {

  private final ObjectMapper objectMapper;
  private final TypeReference<T> type;

  public JacksonBodyHandler(ObjectMapper objectMapper, TypeReference<T> type) {
    this.objectMapper = objectMapper;
    this.type = type;
  }

  @Override
  public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
    return HttpResponse.BodySubscribers.mapping(
        HttpResponse.BodySubscribers.ofInputStream(),
        body -> {
          try {
            return objectMapper.readValue(body, type);
          } catch (IOException e) {
            throw new UncheckedIOException("Unable to deserialize http response", e);
          }
        });
  }
}
