package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.FORM;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.Style.SIMPLE;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.HEADER;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.QUERY;
import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.OperationParameter;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;

public class AstOperationCodeGen {

  private static final Logger LOGGER = getLogger(AstOperationCodeGen.class);

  private static final String ENCODERS = "io.github.tomboyo.lily.http.encoding.Encoders";

  // TODO: make the body parameter part of the ParameterBindings
  // TODO: avoid mutability
  public static Source renderAstOperation(AstOperation ast) {
    var template =
        """
package {{packageName}};
public class {{className}} {
  private final String baseUri;
  private final java.net.http.HttpClient httpClient;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
  {{#hasParameters}}
  private final ParameterBindings parameterBindings;
  {{/hasParameters}}
  {{#bodyFqpt}}
  private {{{bodyFqpt}}} body;
  {{/bodyFqpt}}

  public {{className}}(
      String baseUri,
      java.net.http.HttpClient httpClient,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
    {{!We assume uri is non-null and ends with a trailing '/'.}}
    this.baseUri = baseUri;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    {{#hasParameters}}
    this.parameterBindings = new ParameterBindings();
    {{/hasParameters}}
  }

  {{#hasParameters}}
  public {{className}}(
      String baseUri,
      java.net.http.HttpClient httpClient,
      com.fasterxml.jackson.databind.ObjectMapper objectMapper,
      ParameterBindings parameterBindings) {
    // We assume uri is non-null and ends with a trailing '/'.
    this.baseUri = baseUri;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    {{#hasParameters}}
    this.parameterBindings = parameterBindings;
    {{/hasParameters}}
  }
  {{/hasParameters}}
  {{#hasParameters}}

  /**
  * Return a new instance of this operation with the given parameter bindings.
  */
  public {{className}} withParameters(java.util.function.Function<ParameterBindings, ParameterBindings> p) {
    return new {{className}}(baseUri, httpClient, objectMapper, p.apply(new ParameterBindings()));
  }
  {{/hasParameters}}
  {{#bodyFqpt}}

  /** Configure the request body. */
  public {{className}} body({{{bodyFqpt}}} body) {
    this.body = body;
    return this;
  }
  {{/bodyFqpt}}

  /**
   * Get the base URI of the service (like {@code "https://example.com/"}).
   *
   * <p>It always ends with a trailing slash.</p>
   */
  public String baseUri() {
    return this.baseUri;
  }

  public com.damnhandy.uri.template.UriTemplate relativePathTemplate() {
    return com.damnhandy.uri.template.UriTemplate.buildFromTemplate("{{{pathTemplate}}}").build();
  }

  public String relativePathString() {
    return relativePathTemplate()
        {{#pathParameters}}
        .set("{{{apiName}}}", this.parameterBindings.path.{{name}})
        {{/pathParameters}}
        .expand();
  }

  public com.damnhandy.uri.template.UriTemplate queryTemplate() {
    return com.damnhandy.uri.template.UriTemplate
        .createBuilder()
        .literal("?")
        {{#queryParameters}}
        .{{style}}(
            com.damnhandy.uri.template.UriTemplateBuilder.var("{{{apiName}}}", {{explode}}))
        {{/queryParameters}}
        .build();
  }

  public String queryString() {
    {{#hasQueryParameters}}
    return queryTemplate()
        {{#queryParameters}}
        .set("{{{apiName}}}", this.parameterBindings.query.{{name}})
        {{/queryParameters}}
        .expand();
    {{/hasQueryParameters}}
    {{^hasQueryParameters}}
    return "";
    {{/hasQueryParameters}}
  }

  /**
  * Return an HttpRequest which may be sent directly or further customized with the
  * {@link java.net.http.HttpRequest#newBuilder(java.net.http.HttpRequest, java.util.function.BiPredicate)}} static
  * function.
  */
  public java.net.http.HttpRequest httpRequest() {{#bodyFqpt}}throws com.fasterxml.jackson.core.JsonProcessingException{{/bodyFqpt}} {
  return java.net.http.HttpRequest.newBuilder()
    .uri(java.net.URI.create(this.baseUri + relativePathString() + queryString()))
    .method(
        "{{method}}",
        {{#bodyFqpt}}
        this.body == null
            ? java.net.http.HttpRequest.BodyPublishers.noBody()
            : java.net.http.HttpRequest.BodyPublishers.ofByteArray(
                this.objectMapper.writeValueAsBytes(this.body)))
        {{/bodyFqpt}}
        {{^bodyFqpt}}
        java.net.http.HttpRequest.BodyPublishers.noBody())
        {{/bodyFqpt}}
     {{#bodyFqpt}}
     .header("content-type", "application/json")
     {{/bodyFqpt}}
     {{#headerParameters}}
     .header(
         "{{apiName}}",
         com.damnhandy.uri.template.UriTemplate.createBuilder().{{style}}(
             com.damnhandy.uri.template.UriTemplateBuilder.var("{{{apiName}}}", {{explode}}))
           .build()
           .set("{{{apiName}}}", this.parameterBindings.header.{{name}})
           .expand())
     {{/headerParameters}}
    .build();
  }

  /**
  * Synchronously perform the HTTP request for this operation.
  */
  public {{{responseTypeName}}} sendSync() throws java.io.IOException, InterruptedException {
    return sendSync(httpRequest());
  }

  /**
  * Synchronously perform the HTTP request for a custom HttpRequest. You will typically
  * only use this API when the underlying OpenAPI specification is missing parameters
  * or other necessary components. Use the {@link #httpRequest()} method to get a
  * template HTTP request from this operation, customize it with
  * {@link java.net.http.HttpRequest#newBuilder(java.net.http.HttpRequest, java.util.function.BiPredicate)},
  * then use this method to dispatch it.
  */
  public {{{responseTypeName}}} sendSync(java.net.http.HttpRequest request)
      throws java.io.IOException, InterruptedException {
    var httpResponse = this.httpClient.send(
        request,
        java.net.http.HttpResponse.BodyHandlers.ofInputStream());
    return {{{responseTypeName}}}.fromHttpResponse(httpResponse, objectMapper);
  }

  {{#hasParameters}}
  public static class ParameterBindings {
    public ParameterBindings() {}

    {{#hasQueryParameters}}
    private final Query query = new Query();
    {{/hasQueryParameters}}
    {{#hasPathParameters}}
    private final Path path = new Path();
    {{/hasPathParameters}}
    {{#hasHeaderParameters}}
    private final Header header = new Header();
    {{/hasHeaderParameters}}

    {{#queryParameters}}
    public ParameterBindings {{wither}}({{{fqpt}}} {{name}}) {
      this.query.{{name}} = {{name}};
      return this;
    }
    {{/queryParameters}}
    {{#pathParameters}}
    public ParameterBindings {{wither}}({{{fqpt}}} {{name}}) {
      this.path.{{name}} = {{name}};
      return this;
    }
    {{/pathParameters}}
    {{#headerParameters}}
    public ParameterBindings {{wither}}({{{fqpt}}} {{name}}) {
      this.header.{{name}} = {{name}};
      return this;
    }
    {{/headerParameters}}

    {{#hasQueryParameters}}
    private static class Query {
      private Query() {}

      {{#queryParameters}}
      private {{{fqpt}}} {{name}};
      {{/queryParameters}}
    }
    {{/hasQueryParameters}}
    {{#hasPathParameters}}
    private static class Path {
      private Path() {}

      {{#pathParameters}}
      private {{{fqpt}}} {{name}};
      {{/pathParameters}}
    }
    {{/hasPathParameters}}
    {{#hasHeaderParameters}}
    private static class Header {
      private Header() {}

      {{#headerParameters}}
      private {{{fqpt}}} {{name}};
      {{/headerParameters}}
    }
    {{/hasHeaderParameters}}
  }
  {{/hasParameters}}
}
""";

    /*
    TODO: construct a path template
    In order to do this correctly, I need to not only extract all the path parameters, but keep track of which ones are
    adjacent to one another, e.g. /my/path/{a}{b}{c}/parameters. The reason being that if some chucklefuck decides they
    want form expansion of thsoe threee path parameters, we have to use the {?a,b,c} version of this, not {?a}{?b}{?c}.
    */

    // TODO: I think names should be deduplicated in icg so that we can pass OperationParameter
    // around? If I write a
    //  helper function to handle declarations one at a time, it won't handle duplicated parameter
    // names right.
    var parameterNames = getParameterNames(ast.parameters());
    var pathParameters =
        ast.parameters().stream()
            .filter(parameter -> parameter.location() == PATH)
            .map(
                parameter -> {
                  var name = parameterNames.get(parameter);
                  return Map.of(
                      "fqpt",
                      parameter.typeName().toFqpString(),
                      "name",
                      name.lowerCamelCase(),
                      "wither",
                      "with" + name.upperCamelCase(),
                      "apiName",
                      parameter.apiName(),
                      "style",
                      switch (parameter.encoding().style()) {
                        case SIMPLE, UNSUPPORTED -> "simple";
                        case FORM -> "query";
                      },
                      "explode",
                      parameter.encoding().explode() ? "true" : "false");
                })
            .toList();
    var queryParameters =
        ast.parameters().stream()
            .filter(parameter -> parameter.location() == QUERY)
            .map(
                parameter -> {
                  var name = parameterNames.get(parameter);
                  return Map.of(
                      "fqpt",
                      parameter.typeName().toFqpString(),
                      "name",
                      name.lowerCamelCase(),
                      "wither",
                      "with" + name.upperCamelCase(),
                      "apiName",
                      parameter.apiName(),
                      "style",
                      switch (parameter.encoding().style()) {
                        case SIMPLE -> "simple";
                        case FORM, UNSUPPORTED -> "query";
                      },
                      "explode",
                      parameter.encoding().explode() ? "true" : "false");
                })
            .toList();
    var queryRecordSignature =
        ast.parameters().stream()
            .filter(parameter -> parameter.location() == QUERY)
            .map(
                parameter ->
                    parameter.typeName().toFqpString()
                        + " "
                        + parameterNames.get(parameter).lowerCamelCase())
            .collect(joining(","));
    var queryRecordArguments =
        ast.parameters().stream()
            .filter(parameter -> parameter.location() == QUERY)
            .map(parameterNames::get)
            .map(SimpleName::lowerCamelCase)
            .collect(joining(","));
    var headerParameters =
        ast.parameters().stream()
            .filter(parameter -> parameter.location() == HEADER)
            .map(
                parameter -> {
                  var name = parameterNames.get(parameter);
                  return Map.of(
                      "fqpt",
                      parameter.typeName().toFqpString(),
                      "name",
                      name.lowerCamelCase(),
                      "wither",
                      "with" + name.upperCamelCase(),
                      "apiName",
                      parameter.apiName(),
                      "style",
                      switch (parameter.encoding().style()) {
                        case SIMPLE -> "simple";
                        case FORM, UNSUPPORTED -> "query";
                      },
                      "explode",
                      parameter.encoding().explode() ? "true" : "false");
                })
            .toList();
    var parameters =
        Stream.of(pathParameters.stream(), queryParameters.stream(), headerParameters.stream())
            .flatMap(it -> it)
            .toList();

    var content =
        writeString(
            template,
            "renderAstOperation",
            Map.ofEntries(
                entry("packageName", ast.name().packageName()),
                entry("className", ast.name().typeName()),
                entry("pathTemplate", getPathTemplate(ast)),
                entry("method", ast.method()),
                entry("parameters", parameters),
                entry("hasParameters", !parameters.isEmpty()),
                entry("pathParameters", pathParameters),
                entry("hasPathParameters", !pathParameters.isEmpty()),
                entry("queryParameters", queryParameters),
                entry("hasQueryParameters", !queryParameters.isEmpty()),
                entry("queryRecordSignature", queryRecordSignature),
                entry("queryRecordArguments", queryRecordArguments),
                entry("headerParameters", headerParameters),
                entry("hasHeaderParameters", !headerParameters.isEmpty()),
                entry("responseTypeName", ast.responseName().toFqpString()),
                entry("bodyFqpt", ast.requestBody().<Object>map(Fqn::toFqpString).orElse(false))));

    return new Source(ast.name(), content);
  }

  private static Map<OperationParameter, SimpleName> getParameterNames(
      Collection<OperationParameter> parameters) {
    var parametersByName = parameters.stream().collect(groupingBy(OperationParameter::name));
    return parameters.stream()
        .collect(
            toMap(
                parameter -> parameter,
                // TODO: better Name support. See enum.toString().toLowerCase() which is weird. Do
                // we need to do that?
                parameter ->
                    parametersByName.get(parameter.name()).size() == 1
                        ? parameter.name()
                        : parameter
                            .name()
                            .resolve("In")
                            .resolve(parameter.location().toString().toLowerCase())));
  }

  // TODO: clean this mess up
  /** Get the RFC6570-style path template for this operation */
  private static String getPathTemplate(AstOperation ast) {
    var parametersByApiName =
        ast.parameters().stream()
            .filter(param -> param.location() == PATH)
            .collect(toMap(OperationParameter::apiName, it -> it));
    var matcher = Pattern.compile("\\{[^}]+}").matcher(ast.relativePath());
    var groups = matcher.results().sorted(Comparator.comparingInt(MatchResult::start)).toList();
    // Partition the groups by adjacency of groups and similarity of encoding style (form, simple,
    // etc)
    var acc = new ArrayList<ArrayList<Pair<MatchResult, OperationParameter>>>();
    OperationParameter lastParam = null;
    for (var group : groups) {
      var text = group.group(); // template expression: {example}
      var parameter =
          parametersByApiName.get(text.substring(1, text.length() - 1)); // curly braces removed
      if (lastParam == null // == acc is empty
          || acc.getLast().getLast().left().end() != group.start() // not adjacent?
          || lastParam.encoding().style() != parameter.encoding().style()) { // different style?
        var newlist = new ArrayList<Pair<MatchResult, OperationParameter>>();
        newlist.add(new Pair<>(group, parameter));
        acc.add(newlist);
      } else {
        acc.getLast().add(new Pair<>(group, parameter));
      }
      lastParam = parameter;
    }

    var original = ast.relativePath();
    var template = new StringBuilder();
    int previousEnd = 0;
    for (var styleGroup : acc) {
      // get encoding style for group
      var prefix =
          switch (styleGroup.getFirst().right().encoding().style()) {
            case SIMPLE -> "";
            case FORM -> "?";
            case UNSUPPORTED -> "";
          };
      var body =
          styleGroup.stream()
              .map(
                  pair -> {
                    return pair.right().apiName() + (pair.right().encoding().explode() ? "*" : "");
                  })
              .collect(joining(","));
      // Build up the RFC6570 path template
      // Start by adding the literal path parts between groups of parameters
      template.append(original, previousEnd, styleGroup.getFirst().left().start());
      // add the variable template
      template.append("{%s%s}".formatted(prefix, body));
      previousEnd = styleGroup.getLast().left().end();
    }
    template.append(original.substring(previousEnd)); // the rest of the path template after the last group, if any
    return template.substring(1); // remove leading / present in all paths
  }
}
