package io.github.tomboyo.lily.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

/** Defines a body handler that JSON-deserializes responses to values of a desired type. */
public class JacksonBodyHandler {
  private JacksonBodyHandler() {}

  /**
   * Return a body handler which deserializes JSON objects of the given type using the provided
   * object mapper.
   *
   * @param objectMapper The object mapper with which to read the input stream.
   * @param type The type (reference) of values to deserialize.
   * @param <T> The type of values to deserialize.
   * @return The body handler.
   */
  public static <T> HttpResponse.BodyHandler<Supplier<T>> of(
      ObjectMapper objectMapper, TypeReference<T> type) {
    return (info) ->
        HttpResponse.BodySubscribers.<InputStream, Supplier<T>>mapping(
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
