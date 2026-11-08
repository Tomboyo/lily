package io.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.Lily;
import io.github.tomboyo.lily.http.Options;
import io.github.tomboyo.lily.http.Result;
import io.github.tomboyo.lily.http.model.IParameters;
import io.github.tomboyo.lily.http.model.NoParameters;
import io.github.tomboyo.lily.http.model.Operation;
import io.github.tomboyo.lily.http.model.ParameterBindings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@WireMockTest
public class LilyTest {
  @Test
  void test(WireMockRuntimeInfo info) {
    var options = Options.builder()
        .setHttpClient(HttpClient.newBuilder().build())
        .setBaseUrl(info.getHttpBaseUrl())
        .build();

    Lily.sendSync(
            options,
            Operations.getPet(),
            parameters ->
                parameters
                    .bindPathParameters(p -> p.bindId("mock-id"))
                    .bindQueryParameters(p -> p.bindFoo("foo!").bindBar("bar!").bindBaz("baz!")))
        .reduce(
            ok ->
                switch (ok) {
                  case GetPetResponse.GetPet200 ignored -> Result.ok(200);
                  case GetPetResponse.GetPet404 ignored ->
                      Result.error(new PetStoreError.NoSuchPet());
                },
            err -> Result.error(new PetStoreError.ApiError(err)));
  }

  public sealed interface PetStoreError {
    record NoSuchPet() implements PetStoreError {
    }

    record ApiError(Lily.SendSyncError reason) implements PetStoreError {
    }
  }

  /*
   * Handwritten implementations follow.
   *
   * These are also a "reference" implementation that the generated sources
   * should emulate.
   */

  public static class Operations {
    public static final ObjectMapper MAPPER = new ObjectMapper(); // TODO

    public static Operation<
        NoParameters,
        ParameterBindings<
            GetPetPathParameters,
            GetPetQueryParameters,
            NoParameters,
            NoParameters,
            NoParameters>,
        GetPetResponse>
    getPet() {
      return new Operation<>(
          "GET",
          "/pets/{id}",
          "{?foo,bar,baz,bang}",
          new ParameterBindings<>(
              GetPetPathParameters.empty(),
              GetPetQueryParameters.empty(),
              IParameters.none(),
              IParameters.none(),
              IParameters.none()),
          MAPPER::writeValueAsBytes,
          GetPetResponse::fromHttpResponse);
    }
  }

  public record GetPetPathParameters(String id) implements IParameters {
    public static GetPetPathParameters empty() {
      return new GetPetPathParameters(null);
    }

    public GetPetPathParameters bindId(String id) {
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

  public record GetPetQueryParameters(String foo, String bar, String baz) implements IParameters {
    public static GetPetQueryParameters empty() {
      return new GetPetQueryParameters(null, null, null);
    }

    public GetPetQueryParameters bindFoo(String foo) {
      return new GetPetQueryParameters(foo, bar, baz);
    }

    public GetPetQueryParameters bindBar(String bar) {
      return new GetPetQueryParameters(foo, bar, baz);
    }

    public GetPetQueryParameters bindBaz(String baz) {
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

  public sealed interface GetPetResponse {
    // TODO
    ObjectMapper mapper = new ObjectMapper();

    static Result<GetPetResponse, Lily.SendSyncError.ReadResponseError> fromHttpResponse(HttpResponse<byte[]> httpResponse) {
      try {
        return switch (httpResponse.statusCode()) {
          case 200 -> Result.ok(mapper.readValue(httpResponse.body(), GetPet200.class));
          case 404 -> Result.ok(mapper.readValue(httpResponse.body(), GetPet404.class));
          default ->
              Result.error(new Lily.SendSyncError.ReadResponseError.UnexpectedHttpStatus(httpResponse.statusCode()));
        };
      } catch (IOException e) {
        return Result.error(new Lily.SendSyncError.ReadResponseError.DeserializationError(e));
      }
    }

    record GetPet200() implements GetPetResponse {
    }

    record GetPet404() implements GetPetResponse {
    }
  }
}
