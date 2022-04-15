package com.github.tomboyo.lily.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

/** Deserialize a response body with a Jackson {@Code ObjectMapper}. */
public class JacksonBodyHandler<T> implements HttpResponse.BodyHandler<Supplier<T>> {

  private final ObjectMapper objectMapper;
  private final TypeReference<T> type;

  public JacksonBodyHandler(ObjectMapper objectMapper, TypeReference<T> type) {
    this.objectMapper = objectMapper;
    this.type = type;
  }

  @Override
  public HttpResponse.BodySubscriber<Supplier<T>> apply(HttpResponse.ResponseInfo responseInfo) {
    return HttpResponse.BodySubscribers.<InputStream, Supplier<T>>mapping(
        HttpResponse.BodySubscribers.ofInputStream(),
        (InputStream is) ->
            () -> {
              try (is) {
                return objectMapper.readValue(is, type);
              } catch (IOException e) {
                throw new UncheckedIOException("Unable to deserialize http response", e);
              }
            });
  }
}
