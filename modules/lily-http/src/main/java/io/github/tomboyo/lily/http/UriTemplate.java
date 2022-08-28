package io.github.tomboyo.lily.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UriTemplate {

  private final String template;
  private final HashMap<String, String> parameters;

  private UriTemplate(String template) {
    this.template = template;
    parameters = new HashMap<>();
  }

  public static UriTemplate forPath(String first, String... rest) {
    var uri =
        Stream.concat(Stream.of(first), Arrays.stream(rest))
            .map(UriTemplate::removeLeadingAndTrailingSlash)
            .collect(Collectors.joining("/"));
    return new UriTemplate(uri);
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

  public UriTemplate put(String name, String value) {
    parameters.put(name, value);
    return this;
  }

  /**
   * Create a URI from the given template and interpolated, URL-encoded parameters.
   *
   * @return The finished URI.
   * @throws UriTemplateException If the URI cannot be generated for any reason.
   */
  public URI toURI() {
    var pattern = Pattern.compile("\\{([^{}]+)}"); // "{parameterName}"
    var uri =
        pattern
            .matcher(template)
            .replaceAll(
                (matchResult -> {
                  var name = template.substring(matchResult.start() + 1, matchResult.end() - 1);
                  var value = parameters.get(name);
                  if (value == null) {
                    throw new UriTemplateException("No value set for parameter named " + name);
                  }
                  return URLEncoder.encode(value, UTF_8);
                }));
    return URI.create(uri);
  }
}
