package io.github.tomboyo.lily.http.encoding;

/**
 * A function with formats and URL-encodes given objects, returning a string suitable for
 * constructing URLs.
 *
 * @see Encoders
 * @see io.github.tomboyo.lily.http.UriTemplate
 */
@FunctionalInterface
public interface Encoder {
  /**
   * Produce an appropriate URL-encoded string for the given parameter name and value.
   *
   * @param parameterName The name (key) of the parameter to encode
   * @param value The object to encode
   */
  String encode(String parameterName, Object value);
}
