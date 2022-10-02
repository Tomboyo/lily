package io.github.tomboyo.lily.http;

import io.github.tomboyo.lily.http.encoding.Encoder;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UriTemplate {

  private final String template;
  private final HashMap<String, String> bindings;

  private UriTemplate(String template) {
    this.template = template;
    this.bindings = new HashMap<>();
  }

  /**
   * Create a UriTemplate from one or more strings which are joined together by '/' characters.
   *
   * <p>For example, {@code of("http://foo", "bar/", "/baz"} will return a UriTemplate for the
   * complete path {@code "http://foo/bar/baz}, with all unnecessary slashes removed.
   *
   * @param first The beginning of the URI template.
   * @param rest Subsequent portions of the URI template.
   * @return A UriTemplate for the given template strings.
   */
  public static UriTemplate of(String first, String... rest) {
    if (rest == null) {
      rest = new String[] {};
    }

    var uri =
        Stream.concat(Stream.of(first), Arrays.stream(rest))
            .map(UriTemplate::removeLeadingAndTrailingSlash)
            .collect(Collectors.joining("/"));

    // If the given template ended with a trailing slash, restore that slash.
    if ((rest.length == 0 && first.endsWith("/"))
        || (rest.length > 0 && rest[rest.length - 1].endsWith(("/")))) {
      uri = uri + "/";
    }

    return new UriTemplate(uri);
  }

  /**
   * Bind a URL-encoded string to template parameters with the given name, once per name.
   *
   * @param parameter The name of an unbound template parameter
   * @param value A URL-encoded value
   * @throws IllegalStateException if the parameter has already been bound to a value.
   * @return This instance for chaining.
   */
  public UriTemplate bind(String parameter, String value) {
    if (bindings.put(parameter, value) != null) {
      throw new IllegalStateException("Parameter already bound: name='" + parameter + "'");
    }
    return this;
  }

  /**
   * Bind an object to a template parameter, using the given Encoder to expand the object to a URL-
   * encoded string.
   *
   * @param parameter The name of an unbound template parameter.
   * @param o The value to bind to the parameter.
   * @param encoder The Encoder used to expand the object to a string.
   * @throws IllegalStateException if the parameter has already been bound to a value.
   * @return This instance for chaining.
   */
  public UriTemplate bind(String parameter, Object o, Encoder encoder) {
    if (bindings.put(parameter, encoder.encode(parameter, o)) != null) {
      throw new IllegalStateException("Parameter already bound: name='" + parameter + "'");
    }
    return this;
  }

  /**
   * Remove the value, if any, bound to the template parameter with the given name.
   *
   * @param parameter The name of the template parameter.
   * @return This instance for chaining.
   */
  public UriTemplate unbind(String parameter) {
    bindings.put(parameter, null);
    return this;
  }

  /**
   * Create the finished URI from the template and bound parameters.
   *
   * <p>Empty parameters are encoded as empty strings.
   *
   * @return The finished URI.
   */
  public URI toURI() {
    var pattern = Pattern.compile("\\{([^{}]+)}"); // "{parameterName}"
    var uri =
        pattern
            .matcher(template)
            .replaceAll(
                (matchResult -> {
                  var name = template.substring(matchResult.start() + 1, matchResult.end() - 1);
                  var value = bindings.get(name);
                  if (value == null) {
                    return "";
                  }
                  return value;
                }));
    return URI.create(uri);
  }

  private static String removeLeadingAndTrailingSlash(String part) {
    if (part.codePointAt(0) == '/') {
      part = part.substring(1);
    }

    if (part.codePointAt(part.length() - 1) == '/') {
      part = part.substring(0, part.length() - 1);
    }

    return part;
  }
}
