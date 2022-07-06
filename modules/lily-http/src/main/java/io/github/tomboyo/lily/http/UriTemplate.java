package io.github.tomboyo.lily.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Pattern;

public class UriTemplate {

  private final String template;
  private final HashMap<String, String> parameters;

  private UriTemplate(String template) {
    this.template = template;
    parameters = new HashMap<>();
  }

  public static UriTemplate forPath(String path) {
    return new UriTemplate(path);
  }

  public UriTemplate put(String name, String value) {
    parameters.put(name, value);
    return this;
  }

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
