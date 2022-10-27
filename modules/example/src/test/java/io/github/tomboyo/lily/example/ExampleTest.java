package io.github.tomboyo.lily.example;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.tomboyo.lily.http.encoding.Encoders.formExploded;
import static io.github.tomboyo.lily.http.encoding.Encoders.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.JacksonBodyHandler;
import java.net.http.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
public class ExampleTest {

  /*
   * In this example, we use as much of the Lily generated code support as possible to automate away the complexity of
   * http API integration. If the OAS is malformed or Lily has limited support for a feature, however, the user is
   * always able to access underlying data from the generated API and customize their request arbitrarily.
   */
  @Test
  void happyPath(WireMockRuntimeInfo info) throws Exception {
    /* This wiremock stub imitates the petstore yaml's showPetById operation. */
    stubFor(
        get("/pets/1234")
            .willReturn(ok("""
            {"id": 1234, "name": "Reginald"}
            """)));

    /* Use the API to build a well-formed URI to the showPetById operation for resource 1234. At time of writing, the
     * user must then use the underlying uriTemplate to retrieve the complete URI and send the request manually with
     * java.net.http.
     *
     * Header and cookie parameters are not yet supported. These must be set manually.
     */
    var api =
        Api.newBuilder()
            .uri(info.getHttpBaseUrl())
            // .httpClient(httpClient) // optional: inject a customized client.
            .build();
    var request =
        api.petsOperations() // All operations with the `pets` tag. (We could also use
            // .allOperations())
            .showPetById() // The GET /pets/{petId} operation
            .petId("1234") // bind "1234" to the {petId} parameter of the OAS operation
            .httpRequest(); // Access the templated httpRequest.

    /* Finish and send the request manually. Note the use of the generated Pet type. All component and path parameter
     * schema are generated. Also note the use of the provided lily-http JacksonBodyHandler
     */
    var response =
        api.httpClient()
            .send(request, JacksonBodyHandler.of(new ObjectMapper(), new TypeReference<Pet>() {}));

    assertEquals(200, response.statusCode());
    assertEquals(new Pet(1234L, "Reginald", null), response.body().get());
  }

  /* This test case demonstrates ways a user can dip below the generated API to directly manipulate HTTP requests. This
   * may be necessary whenever the source OAS document is incomplete (or wrong), or whenever Lily does not support a
   * feature.
   */
  @Test
  void manual(WireMockRuntimeInfo info) throws Exception {
    /* This wiremock stub imitates the petstore yaml's showPetById operation. */
    stubFor(
        get("/pets/1234?foo=foo&bar=bar")
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
    var api = Api.newBuilder().uri(info.getHttpBaseUrl()).build();
    var operation =
        api
            // All operations with the `pets` tag. (We could also use .allOperations())
            .petsOperations()
            .showPetById(); // the GET /pets/{petId} operation

    var uri =
        operation
            // Access the underlying uri template to finish the request manually
            .uriTemplate()
            // Bind "1234" to the petId path parameter. The Encoders class implements several common
            // formats. We can override bindings set by the operation.
            .bind("petId", 1234, simple())
            // The operation doesn't have any query parameter templates, so we'll add one and bind a
            // value to it.
            .appendTemplate("{queryParameters}")
            .bind("queryParameters", Map.of("foo", "foo", "bar", "bar"), formExploded())
            .toURI();

    /*
     * Finish and send the request manually. Note the use of the generated Pet type. All components schemas and
     * parameter schemas are generated. Also note the use of the provided lily-http JacksonBodyHandler.
     */
    var response =
        api.httpClient()
            .send(
                HttpRequest.newBuilder().GET().uri(uri).build(),
                JacksonBodyHandler.of(new ObjectMapper(), new TypeReference<Pet>() {}));

    assertEquals(200, response.statusCode());
    assertEquals(new Pet(1234L, "Reginald", null), response.body().get());
  }
}
