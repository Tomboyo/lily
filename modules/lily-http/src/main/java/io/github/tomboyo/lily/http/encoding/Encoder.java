package io.github.tomboyo.lily.http.encoding;

@FunctionalInterface
public interface Encoder {
  /** Produce an appropriate URL-encoded string for the given parameter name and value. */
  String encode(String parameterName, Object value);
}
