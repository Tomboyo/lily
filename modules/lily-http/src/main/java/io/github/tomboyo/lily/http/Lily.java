package io.github.tomboyo.lily.http;

import com.damnhandy.uri.template.UriTemplate;
import io.github.tomboyo.lily.http.model.NoParameters;
import io.github.tomboyo.lily.http.model.Operation;
import io.github.tomboyo.lily.http.model.ParameterBindings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.function.Function;

public class Lily {

  public static <Body, Parameters extends ParameterBindings<?, ?, ?, ?, Body>, Response>
      Result<Response, SendSyncError> sendSync(
          Options options,
          Operation<Body, Parameters, Response> operation,
          Function<Parameters, Parameters> f) {
    var request = createHttpRequest(options.baseUrl(), operation, f);
    var reader = operation.responseReader();
    return sendSync(options.httpClient(), request).flatMap(reader::readResponse);
  }

  private static Result<HttpResponse<byte[]>, SendSyncError> sendSync(
      HttpClient client, HttpRequest request) {
    try {
      return Result.ok(client.send(request, HttpResponse.BodyHandlers.ofByteArray()));
    } catch (HttpConnectTimeoutException e) {
      return Result.error(new SendSyncError.ConnectTimeout(e));
    } catch (HttpTimeoutException e) {
      return Result.error(new SendSyncError.ResponseTimeout(e));
    } catch (IOException e) {
      return Result.error(new SendSyncError.IoError(e));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Result.error(new SendSyncError.InterruptedError(e));
    }
  }

  private static <Body, Parameters extends ParameterBindings<?, ?, ?, ?, Body>, Response>
      HttpRequest createHttpRequest(
          String baseUrl,
          Operation<Body, Parameters, Response> operation,
          Function<Parameters, Parameters> f) {
    var requestTemplate = f.apply(operation.parameters());

    /* Path and query fragments are expanded independently since parameter
    names are only unique down to their name _and_ location. If we mixed
    them all up together into one template, a path parameter and a query
    parameter may collide. */

    /* All path parameters must be RFC6570 serialized. The operation is
    generated with an RFC6570 template. */
    var pathFragment =
        UriTemplate.fromTemplate(operation.pathTemplate())
            .set(requestTemplate.pathParameters().asMap())
            .expand();

    // TODO: query
    var queryFragment = "";

    /* NOTE: the path fragment always begins with a slash. The query should likewise begin with
    '?'. */
    var uri = URI.create(baseUrl + pathFragment + queryFragment);

    // TODO: headers, see above comment about format+style/content encoding
    /*
    for (var entry : requestTemplate.headerParameters().asMap().entrySet()) {
      requestBuilder = requestBuilder.header(entry.getKey(), todo(entry.getValue()));
    }
    */

    // TODO: cookies

    HttpRequest.BodyPublisher bodyPublisher;
    try {
      bodyPublisher =
          requestTemplate.bodyParameters() instanceof NoParameters
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofByteArray(
                  operation.bodyWriter().writeBody(requestTemplate.bodyParameters()));
    } catch (Exception e) {
      throw new ApiException("Unable to serialize the http request body", e);
    }

    return HttpRequest.newBuilder().method(operation.httpMethod(), bodyPublisher).uri(uri).build();
  }

  public sealed interface SendSyncError {
    record ConnectTimeout(HttpConnectTimeoutException exception) implements SendSyncError {}

    record InterruptedError(InterruptedException exception) implements SendSyncError {}

    record IoError(IOException exception) implements SendSyncError {}

    record ResponseTimeout(HttpTimeoutException exception) implements SendSyncError {}

    sealed interface ReadResponseError extends SendSyncError {
      record DeserializationError(IOException exception) implements ReadResponseError {}

      record UnexpectedHttpStatus(int code, HttpResponse<?> response) implements ReadResponseError {}
    }
  }
}
