package io.github.tomboyo.lily.http;

import static java.util.Objects.requireNonNullElse;

import io.github.tomboyo.lily.http.encoding.Encoder;
import java.net.URI;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * A utility that creates URIs from template strings and parameter bindings.
 *
 * <p>
 *
 * <pre>
 *   UriTemplate
 *     .of("https://example.com/", "{myParam}", "{query}{continuation}")
 *     .bind("myParam", "some;value")
 *     .bind("query", Map.of("key1", "value1"), Encoding.form(EXPLODE))
 *     .bind("continuation", List.of("a", "b"), Encoding.formContinuation(EXPLODE))
 *     .toURI()
 *     .toString();
 *     // => https://example.com/some;value?/?key1=value1&continuation=a&continuation=b
 * </pre>
 */
public class UriTemplate {

  private final String template;
  private final HashMap<String, String> bindings;

  private UriTemplate(String template, HashMap<String, String> bindings) {
    this.template = template;
    this.bindings = bindings;
  }

  /**
   * Create a UriTemplate from the given string.
   *
   * @param template The template string.
   * @return A UriTemplate for the given template strings.
   */
  // Note: we do not support a `String first, String... rest` API because some scenarios are
  // ambiguous; consider `of("http://example.com/", "{pathParameter}", "{queryParameter}")`.
  public static UriTemplate of(String template) {
    return new UriTemplate(template, new HashMap<>());
  }

  /**
   * Return a new UriTemplate with the same parameter bindings but a new template string.
   *
   * @param template The new template string.
   * @return A new UriTemplate instance.
   */
  public UriTemplate withTemplate(String template) {
    return new UriTemplate(template, bindings);
  }

  /**
   * Return a new UriTemplate with an extended template string and the same parameter bindings.
   *
   * @param more The string to append to the current template.
   * @return A new UriTemplate instance.
   */
  public UriTemplate appendTemplate(String more) {
    return new UriTemplate(template + more, bindings);
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
                  return requireNonNullElse(value, "");
                }));
    return URI.create(uri);
  }
}
