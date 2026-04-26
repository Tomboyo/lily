package io.github.tomboyo.lily.http;

import com.damnhandy.uri.template.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class Api {
  public static <
      Request extends RequestTemplate<?, ?>,
      Response
  > Response sendSync(
      HttpClient client,
      String baseUrl,
      Operation<Request, Response> operation,
      Function<Request, Request> f
  ) throws InterruptedException {
    try {
      var requestTemplate= f.apply(operation.requestTemplate());

      var uriTemplate = UriTemplate.fromTemplate(
          // TODO: trailing-slash-aware url-join
          baseUrl + requestTemplate.uriTemplate());
      if (!(requestTemplate.pathParameters() instanceof NoPathParameters)) {
        uriTemplate.set(requestTemplate.pathParameters().asMap());
      }
      if (!(requestTemplate.queryParameters() instanceof NoQueryParameters)) {
        uriTemplate.set(requestTemplate.queryParameters().asMap());
      }

      var request = HttpRequest.newBuilder()
          .uri(URI.create(uriTemplate.expand()))
          .build();
      var response = client.send(
          request,
          HttpResponse.BodyHandlers.ofByteArray());
      return operation.responseReader().readResponse(response);
    } catch (IOException e) {
      throw new RuntimeException("Unable to make HTTP request", e);
    }
  }

  public record Operation<
      Request extends IRequestTemplate<?, ?>,
      Response
  >(
      Request requestTemplate,
      ResponseReader<Response> responseReader
  ) {
    public Operation<Request, Response> withRequestTemplate(Request customTemplate) {
      return new Operation<>(customTemplate, responseReader);
    }

    public <Response2> Operation<Request, Response2> withResponseReader(ResponseReader<Response2> customResponseReader) {
      return new Operation<>(requestTemplate, customResponseReader);
    }
  }

  public interface IRequestTemplate<
      PathParameters extends IPathParameters,
      QueryParameters extends IQueryParameters
  > {
    IRequestTemplate<PathParameters, QueryParameters> withPathParameters(Function<PathParameters, PathParameters> f);
    IRequestTemplate<PathParameters, QueryParameters> withQueryParameters(Function<QueryParameters, QueryParameters> f);
  }

  public record RequestTemplate<
      PathParameters extends IPathParameters,
      QueryParameters extends IQueryParameters
  > (
      String uriTemplate,
      PathParameters pathParameters,
      QueryParameters queryParameters
  ) implements IRequestTemplate<PathParameters, QueryParameters> {
    @Override
    public RequestTemplate<PathParameters, QueryParameters> withPathParameters(Function<PathParameters, PathParameters> f) {
      return new RequestTemplate<>(uriTemplate, f.apply(pathParameters), queryParameters);
    }

    @Override
    public IRequestTemplate<PathParameters, QueryParameters> withQueryParameters(Function<QueryParameters, QueryParameters> f) {
      return new RequestTemplate<>(uriTemplate, pathParameters, f.apply(queryParameters));
    }
  }

  @FunctionalInterface
  public interface ResponseReader<T> {
    T readResponse(HttpResponse<byte[]> httpResponse);
  }

  public interface IPathParameters extends IParameters {}
  public interface IQueryParameters extends IParameters {}
  public interface IParameters {
    Map<String, Object> asMap();
  }

  public static class NoPathParameters implements IPathParameters {
    @Override
    public Map<String, Object> asMap() {
      return Map.of();
    }
  }

  public static class NoQueryParameters implements IQueryParameters {
    @Override
    public Map<String, Object> asMap() {
      return Map.of();
    }
  }

  // Example Impls

  public class Operations{
    public Operation<
        RequestTemplate<GetPetPathParameters, GetPetQueryParameters>,
        GetPetResponse> getPet() {
      return new Operation<>(
          new RequestTemplate<>(
              "/pets/{id}{?foo,bar,baz}",
              GetPetPathParameters.empty(),
              GetPetQueryParameters.empty()),
          (httpResponse) -> new GetPetResponse());
    }
  }

  public record GetPetPathParameters(String id) implements IPathParameters {
    public static GetPetPathParameters empty() {
      return new GetPetPathParameters(null);
    }

    public GetPetPathParameters withId(String id) {
      return new GetPetPathParameters(id);
    }

    @Override
    public Map<String, Object> asMap() {
      var acc = new HashMap<String, Object>();
      if (id != null) {
        acc.put("id", id);
      }
      return acc;
    }
  }

  public record GetPetQueryParameters(String foo, String bar, String baz) implements IQueryParameters {
    public static GetPetQueryParameters empty() {
      return new GetPetQueryParameters(null, null, null);
    }

    public GetPetQueryParameters withFoo(String foo) {
      return new GetPetQueryParameters(foo, bar, baz);
    }

    public GetPetQueryParameters withBar(String bar) {
      return new GetPetQueryParameters(foo, bar, baz);
    }

    public GetPetQueryParameters withBaz(String baz) {
      return new GetPetQueryParameters(foo, bar, baz);
    }

    @Override
    public Map<String, Object> asMap() {
      var acc = new HashMap<String, Object>();
      if (foo != null) {
        acc.put("foo", foo);
      }
      if (bar != null) {
        acc.put("bar", bar);
      }
      if (baz != null) {
        acc.put("baz", baz);
      }
      return acc;
    }
  }

  // Would normally be a sealed interface
  public class GetPetResponse {}

  // Customizations!

  /* OLD APPROACH
  public static <Template extends ITemplate, Response> Result<Response> sendSync(
      HttpClient httpClient,
      String baseUrl,
      IExchange<Template, Response> exchange,
      Function<Template, Template> fillTemplate
  ) throws InterruptedException {
    try {
      var template = fillTemplate.apply(exchange.createRequestTemplate());
      var httpRequest = HttpRequests.builder(baseUrl, template).build();
      var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
      var response = exchange.readResponse(httpResponse);
      return new Result.Ok<>(response);
    } catch (IOException e) {
      return new Result.Error<>("Unable to send HTTP request", e);
    }
  }

  public interface IExchange<Template extends ITemplate, Response> {
    Template createRequestTemplate();
    Response readResponse(HttpResponse<byte[]> httpResponse);
  }

  // Marker interface only.
  public interface ITemplate {}

  public interface IHasPathParameters<Path extends IPathParameters> extends ITemplate {
    <Path2 extends IPathParameters> IHasPathParameters<Path2> withPathParameters(Path2 pathParameters);
    IHasPathParameters<Path> withPathOverride(String pathOverride);

    Path pathParameters();
    Optional<String> pathOverride();

    default <Path2 extends IPathParameters> IHasPathParameters<Path2> withPathParameters(
        Function<? super Path, Path2> f) {
      return withPathParameters(f.apply(pathParameters()));
    }

    default <Path2 extends IPathParameters> IHasPathParameters<Path2> withPathParameters(
        Function<? super Path2, Path2> f,
        Supplier<Path2> g) {
      return withPathParameters(f.apply(g.get()));
    }
  }

  public interface IPathParameters {
    Map<String, String> asMap();
  }

  public static class HttpRequests {
    public static HttpRequest.Builder builder(String baseUrl, ITemplate template) {
      return HttpRequest.newBuilder();
          //.uri(...)
    }
  }

  public sealed interface Result<T> permits Result.Ok, Result.Error {
    record Ok<T>(T result) implements Result<T> {}
    record Error<T>(String message, Exception cause) implements Result<T> {}
  }

  // EXAMPLE IMPLS

  // NOTE: templates are immutable, so this could in fact be a record.
  static class MyGetPetExchange implements IExchange<GetPetTemplate<GetPetTemplate.PathParameters>, GetPetResponse> {
    @Override
    public GetPetTemplate<GetPetTemplate.PathParameters> createRequestTemplate() {
      return GetPetTemplate.defaultInstance();
    }

    @Override
    public GetPetResponse readResponse(HttpResponse<byte[]> httpResponse) {
      return GetPetResponse.read(httpResponse);
    }
  }

  static class GetPetTemplate<Path extends IPathParameters> implements
      IHasPathParameters<Path> {
    private final Path pathParameters;
    private final Optional<String> pathOverride;

    public static GetPetTemplate<PathParameters> defaultInstance() {
      return new GetPetTemplate<>(new PathParameters(), Optional.empty());
    }

    public GetPetTemplate(Path pathParameters, Optional<String> pathOverride) {
      this.pathParameters = pathParameters;
      this.pathOverride = pathOverride;
    }

    @Override
    public <Path2 extends IPathParameters> GetPetTemplate<Path2> withPathParameters(Path2 pathParameters) {
      return new GetPetTemplate<>(pathParameters, pathOverride);
    }

    @Override
    public IHasPathParameters<Path> withPathOverride(String pathOverride) {
      return new GetPetTemplate<>(pathParameters, Optional.of(pathOverride));
    }

    @Override
    public Path pathParameters() {
      return pathParameters;
    }

    @Override
    public Optional<String> pathOverride() {
      return Optional.empty();
    }

    static class PathParameters implements IPathParameters {
      @Override
      public Map<String, String> asMap() {
        return Map.of();
      }
    }
  }

  record GetPetResponse() {
    public static GetPetResponse read(HttpResponse<byte[]> httpResponse) {
      return null;
    }
  }

  static class MyGetPetTemplate<
      Path extends GetPetTemplate.PathParameters,
      Query extends MyGetPetTemplate.QueryParameters> extends GetPetTemplate<Path> {

    private final Supplier<Query> newQueryParameters;
    private final Query queryParameters;

    public MyGetPetTemplate(
        Supplier<Path> newPathParameters,
        Path pathParameters,
        Supplier<Query> newQueryParameters,
        Query queryParameters) {
      super(newPathParameters, pathParameters);

      this.newQueryParameters = newQueryParameters;
      this.queryParameters = queryParameters;
    }

    public static class QueryParameters {}
  }
  */
}
