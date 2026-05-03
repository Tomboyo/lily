package io.github.tomboyo.lily.http;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public class Api {
  /*
   * TODO:
   *  - capture documentation on the difference between Result and
   *    RuntimeException. The gist is: Result<Ok, Error> is for all reasonable
   *    things a function could do that the code SHOULD expect and handle.
   *    RuntimeExceptions are _bugs_. There's no reasonable way to catch and
   *    handle a RuntimeException, because it means the code is flawed and donig
   *    the wrong thing -- the way to "handle" a bug is to FIX it. So, throw RTE
   *    when the user needs to re-code something (or implement a custom bug-free
   *    implementation of otherwise generated code), and return a Result for
   *    normal control flow.
   * - work on cookie management. I think the user needs to provide one cookie
   *   manager per http client per "session," and pass the correct client in
   *   when they need a particular set of sessions. We need a way to interact
   *   with the cookie manager from within this function.
   * NOTE:
   * - throwing unchecked exceptions. These represent a flaw in the logic, not a
   *   value that could be sensibly used for control flow. If the user sees
   *   these exceptions in their logs, it may prompt them to implement
   *   workaround code. That code would be a new Operation definition, not
   *   something that could consume the failure and try to handle it as a
   *   fallback. This API isn't polluted with code to aid control flow in the
   *   worst case. The user can extend lily to implement the operation correctly
   *   instead.
   * - This is DIFFERENT from the various result types one-to-one with response
   *   codes. Each code represents an expected and normal type of response, e.g.
   *   200 (content) versus 404 (no content).
   */
  public static <
      BodyParameters,
      Parameters extends ParameterBindings<?, ?, ?, BodyParameters>,
      Response
  > Result<Response, SendSyncError> sendSync(
      HttpClient client,
      String baseUrl,
      Operation<BodyParameters, Parameters, Response> operation,
      Function<Parameters, Parameters> f
  ) throws InterruptedException {
      var requestTemplate = f.apply(operation.parameters());

      /* Pah and query fragments are expanded independently since parameter
         names are only unique down to their name _and_ location. If we mixed
         them all up together into one template, a path parameter and a query
         parameter may collide. */
      var pathFragment = UriTemplate.fromTemplate(operation.pathTemplate())
          .set(requestTemplate.pathParameters().asMap())
          .expand();
      var queryFragment = UriTemplate.fromTemplate(operation.queryTemplate())
          .set(requestTemplate.queryParameters().asMap())
          .expand();
      // TODO: how to joint baseUrl with path fragment? Enforce trailing slash?
      var uri = URI.create(baseUrl + pathFragment + queryFragment);

      HttpRequest.BodyPublisher bodyPublisher;
      try {
        bodyPublisher = requestTemplate.bodyParameters() instanceof NoBodyParameters
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(
                operation.bodyWriter().writeBody(requestTemplate.bodyParameters()));
      } catch (Exception e) {
        throw new ApiException("Unable to serialize the http request body", e);
      }

      var requestBuilder = HttpRequest.newBuilder()
          .method(operation.httpMethod(), bodyPublisher)
          .uri(uri);

      for (var entry : requestTemplate.headerParameters().asMap().entrySet()) {
        requestBuilder = requestBuilder.header(entry.getKey(), entry.getValue());
      }

      HttpResponse<byte[]> httpResponse;
      try {
        httpResponse = client.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofByteArray());
      } catch (HttpConnectTimeoutException e) {
        return new Result.Error<>(new SendSyncError.ConnectTimeout(e));
      } catch (HttpTimeoutException e) {
        return new Result.Error<>(new SendSyncError.ResponseTimeout(e));
      } catch (IOException e) {
        return new Result.Error<>(new SendSyncError.IoError(e));
      }

      try {
        return new Result.Ok<>(
            operation.responseReader().readResponse(httpResponse));
      } catch (Exception e) {
        throw new ApiException("Unable to deserialize the http response", e);
      }
  }

  public sealed interface SendSyncError {
    <E extends Exception> E exception();

    record ConnectTimeout(HttpConnectTimeoutException exception) implements SendSyncError {}
    record ResponseTimeout(HttpTimeoutException exception) implements SendSyncError {}
    record IoError(IOException exception) implements SendSyncError {}
  }

  /** Thrown due to an unrecoverable, unexpected exception while preparing or
    * sending an HTTP request.
    */
  public static final class ApiException extends RuntimeException {
    public ApiException(String message) {
      super(message);
    }

    public ApiException(String message, Exception cause) {
      super(message, cause);
    }
  }

  public record Operation<
      BodyParameters,
      Parameters extends ParameterBindings<?, ?, ?, BodyParameters>,
      Response
  >(
      String httpMethod,
      String pathTemplate,
      String queryTemplate,
      Parameters parameters,
      BodyWriter<BodyParameters> bodyWriter,
      ResponseReader<Response> responseReader
  ) {
    public Operation<BodyParameters, Parameters, Response> withRequestTemplate(Parameters customTemplate) {
      return new Operation<>(httpMethod, pathTemplate, queryTemplate, customTemplate, bodyWriter, responseReader);
    }

    public <Response2> Operation<BodyParameters, Parameters, Response2> withResponseReader(ResponseReader<Response2> customResponseReader) {
      return new Operation<>(httpMethod, pathTemplate, queryTemplate, parameters, bodyWriter, customResponseReader);
    }
  }

  @FunctionalInterface
  public interface BodyWriter<BodyParameters> {
    byte[] writeBody(BodyParameters parameters) throws Exception;
  }

  @FunctionalInterface
  public interface ResponseReader<T> {
    T readResponse(HttpResponse<byte[]> httpResponse) throws Exception;
  }

  public record ParameterBindings<
      PathParameters extends IKvParameters<String, Object>,
      QueryParameters extends IKvParameters<String, Object>,
      HeaderParameters extends IKvParameters<String, String>,
      BodyParameters
  > (
      PathParameters pathParameters,
      QueryParameters queryParameters,
      HeaderParameters headerParameters,
      BodyParameters bodyParameters
  ) {
    public ParameterBindings<PathParameters, QueryParameters, HeaderParameters, BodyParameters> withPathParameters(Function<PathParameters, PathParameters> f) {
      return new ParameterBindings<>(f.apply(pathParameters), queryParameters, headerParameters, bodyParameters);
    }

    public ParameterBindings<PathParameters, QueryParameters, HeaderParameters, BodyParameters> withQueryParameters(Function<QueryParameters, QueryParameters> f) {
      return new ParameterBindings<>(pathParameters, f.apply(queryParameters), headerParameters, bodyParameters);
    }
  }

  /** Key-value pairs for path, query, header, and cookie parameters. */
  public interface IKvParameters<Key, Value> {
    Map<Key, Value> asMap();
  }

  /** Default/Empty impl of IKvParameters for APIs without parameters. */
  public static class NoKvParameters<Key, Value> implements IKvParameters<Key, Value> {
    public static <Key, Value> NoKvParameters<Key, Value> empty() {
      return new NoKvParameters<>();
    }

    @Override
    public Map<Key, Value> asMap() {
      return Map.of();
    }
  }

  public static class NoBodyParameters {}


  //
  // Utils
  //

  public sealed interface Result<T, E> permits Result.Ok, Result.Error {
    <T2> Result<T2, E> map(Function<T, T2> f);
    <E2> Result<T, E2> mapError(Function<E, E2> f);
    <T2, E2> Result<T2, E2> biMap(Function<T, T2> fOk, Function<E, E2> fError);
    <R> R unwrap(Function<T, R> fOk, Function<E, R> fError);

    record Ok<T, E>(T value) implements Result<T, E> {
      @Override
      public <T2> Result<T2, E> map(Function<T, T2> f) {
        return new Ok<>(f.apply(value));
      }

      @Override
      @SuppressWarnings("unchecked")
      public <E2> Result<T, E2> mapError(Function<E, E2> f) {
        return (Result<T, E2>) this;
      }

      @Override
      public <T2, E2> Result<T2, E2> biMap(Function<T, T2> fOk, Function<E, E2> fError) {
        return new Ok<>(fOk.apply(value));
      }

      @Override
      public <R> R unwrap(Function<T, R> fOk, Function<E, R> fError) {
        return fOk.apply(value);
      }
    }

    record Error<T, E>(E value) implements Result<T, E> {
      @Override
      @SuppressWarnings("unchecked")
      public <T2> Result<T2, E> map(Function<T, T2> f) {
        return (Error<T2, E>) this;
      }

      @Override
      public <E2> Result<T, E2> mapError(Function<E, E2> f) {
        return new Error<>(f.apply(value));
      }

      @Override
      public <T2, E2> Result<T2, E2> biMap(Function<T, T2> fOk, Function<E, E2> fError) {
        return new Error<>(fError.apply(value));
      }

      @Override
      public <R> R unwrap(Function<T, R> fOk, Function<E, R> fError) {
        return fError.apply(value);
      }
    }
  }

  //
  // Example Impls
  //

  public static class Operations{
    public static final ObjectMapper MAPPER = new ObjectMapper(); // TODO
    public static Operation<
        NoBodyParameters,
        ParameterBindings<GetPetPathParameters, GetPetQueryParameters, NoKvParameters<String, String>, NoBodyParameters>,
        GetPetResponse> getPet() {
      return new Operation<>(
          "GET",
          "/pets/{id}",
          "{?foo,bar,baz}",
          new ParameterBindings<>(
              GetPetPathParameters.empty(),
              GetPetQueryParameters.empty(),
              NoKvParameters.empty(),
              new NoBodyParameters()),
          MAPPER::writeValueAsBytes,
          GetPetResponse::fromHttpResponse);
    }
  }

  public record GetPetPathParameters(String id) implements IKvParameters<String, Object> {
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

  public record GetPetQueryParameters(String foo, String bar, String baz) implements IKvParameters<String, Object> {
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

  public sealed interface GetPetResponse permits GetPet200, GetPet404 {
    // TODO
    static final ObjectMapper mapper = new ObjectMapper();

    static GetPetResponse fromHttpResponse(HttpResponse<byte[]> httpResponse) throws IOException {
      return switch (httpResponse.statusCode()) {
        case 200 -> mapper.readValue(httpResponse.body(), GetPet200.class);
        case 404 -> mapper.readValue(httpResponse.body(), GetPet404.class);
        default -> throw new IOException("Unexpected http status '%s'".formatted(httpResponse.statusCode()));
      };
    }
  }
  public record GetPet200() implements GetPetResponse {}
  public record GetPet404() implements GetPetResponse {}
}
