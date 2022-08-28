package io.github.tomboyo.lily.example;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.JacksonBodyHandler;
import io.github.tomboyo.lily.http.encoding.Encoding;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

@WireMockTest
public class Example {

  @Test
  void example(WireMockRuntimeInfo info) throws Exception {
    /* This wiremock stub imitates the petstore yaml's showPetById operation. */
    stubFor(
        get("/foo/pets/1234")
            .willReturn(ok("""
            {"id": 1234, "name": "Reginald"}
            """)));

    /*
     * All operations (GET /foo, POST /foo, etc) documented by the OAS are captured by the generated API, named
     * according to their OAS operation IDs.
     *
     * All operations are organized by their tags, if any, like: `petsOperations()` (for the 'pets' tag),
     * `otherOperations()` (for operations without a tag), and `allOperations()` (every operation is addressable from
     * here, even if already addressable from another tag).
     */
    var operation =
        Api.newBuilder().uri(info.getHttpBaseUrl()).build().petsOperations().showPetById();

    /*
     * Every operation exposes a uriTemplate which supports parameter interpolation. It may be used to create a URI for
     * a specific operation. This capability will always exist as a means to work around OAS and Lily flaws. Currently,
     * this is the only option -- Lily will support builders at the operation API level in the future to hide this
     * complexity.
     *
     * Note the use of Encoding, which has support for different parameter encoding strategies like "simple" for path
     * parameters and "formExplode" for query parameters.
     */
    var uri = operation.uriTemplate().put("petId", Encoding.simple(1234)).toURI();

    /*
     * The user can construct a native java 11+ java.net.http request using the templated URI and lily-http helpers like
     * the JacksonBodyHandler. Eventually, this will happen automatically, but will always be exposed to work around
     * flaws in the OAS or Lily.
     *
     * Note the use of the generated Pet type. All components schemas and parameter schemas are generated! No hand-
     * writing models!
     */
    var client = HttpClient.newBuilder().build();
    var response =
        client.send(
            HttpRequest.newBuilder().GET().uri(uri).build(),
            JacksonBodyHandler.of(new ObjectMapper(), new TypeReference<Pet>() {}));

    assertEquals(200, response.statusCode());
    assertEquals(new Pet(1234L, "Reginald", null), response.body().get());
  }
}
