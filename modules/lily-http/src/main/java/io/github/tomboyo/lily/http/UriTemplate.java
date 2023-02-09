package io.github.tomboyo.lily.http;

import static java.util.Objects.requireNonNullElse;

import io.github.tomboyo.lily.http.encoding.Encoder;
import io.github.tomboyo.lily.http.encoding.Encoders;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * A utility that creates strings from templates and parameter bindings, where all bindings are URL-
 * encoded.
 *
 * <pre>{@code
 * UriTemplate
 *   .of("https://example.com/{myParam}/{query}{continuation}")
 *   .bind("myParam", "some;value")
 *   .bind("query", Map.of("key", "value?"), Encoders.form(EXPLODE))
 *   .bind("continuation", List.of("a", "b"), Encoders.formContinuation(EXPLODE))
 *   .toString();
 *   // => https://example.com/some;value/?key=value%3F&continuation=a&continuation=b
 * }</pre>
 *
 * @see Encoders
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
  public static UriTemplate of(String template) {
    return new UriTemplate(template, new HashMap<>());
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
   * @param value The value to bind to the parameter.
   * @param encoder The Encoder used to expand the object to a string.
   * @throws IllegalStateException if the parameter has already been bound to a value.
   * @return This instance for chaining.
   */
  public UriTemplate bind(String parameter, Object value, Encoder encoder) {
    if (bindings.put(parameter, encoder.encode(parameter, value)) != null) {
      throw new IllegalStateException("Parameter already bound: name='" + parameter + "'");
    }
    return this;
  }

  /**
   * Create teh interpolated string from the template and bound parameters.
   *
   * <p>Empty parameters are encoded as empty strings.
   *
   * @return The interpolated string.
   */
  @Override
  public String toString() {
    var pattern = Pattern.compile("\\{([^{}]+)}"); // "{parameterName}"
    return pattern
        .matcher(template)
        .replaceAll(
            (matchResult -> {
              var name = template.substring(matchResult.start() + 1, matchResult.end() - 1);
              var value = bindings.get(name);
              return requireNonNullElse(value, "");
            }));
  }
}
