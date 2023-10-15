package io.github.tomboyo.lily.example;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.example.showpetbyidoperation.ShowPetById200;
import io.github.tomboyo.lily.example.showpetbyidoperation.ShowPetByIdDefault;
import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

/* These are examples of how to use the API, formulated as contrived unit tests. These are not actual tests. */
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

    /* First, create an API instance, which is safe to share. This will typically be a singleton. */
    var api =
        Api.newBuilder()
            .uri(info.getHttpBaseUrl())
            // .httpClient(httpClient) // optional: inject a customized client.
            .build();

    /* Then, use the fluent API to configure and execute the show-pet-by-id operation. At time of writing, we're able to
     * fluently specify path and query parameters (not yet header or body parameters).
     */
    var response =
        // All operations with the `pets` tag. (see also: everyOperation)
        api.petsOperations()
            // The GET /pets/{petId} operation
            .showPetById()
            // bind "1234" to the {petId} path parameter of the OAS operation.
            .path(path -> path.petId("1234"))
            // execute the request synchronously and get a ShowPetByIdResponse object.
            .sendSync();

    /* The response object is a sealed interface based on what the OAS says the API can return. In this case, the
     * ShowPetByIdResponse may consist of the 200 and Default variants.
     */
    if (response instanceof ShowPetById200 okResponse) {
      assertEquals(Pet.newBuilder().setId(1234L).setName("Reginald").build(), okResponse.body());
    } else if (response instanceof ShowPetByIdDefault) {
      fail("Expected 200.");
    } else {
      fail("Expected 200.");
    }

    /* We can also access the java.net.http.HttpResponse<? extends InputStream> directly from the sealed interface. */
    assertEquals(200, response.httpResponse().statusCode());
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

    var api = Api.newBuilder().uri(info.getHttpBaseUrl()).build();

    /* Customize a request with any parameters documented by the OpenAPI specification. In this
     * example we'll assume query parameters are missing from the OpenAPI specification and can't
     * be configured via the generated API.
     */
    var operation = api.petsOperations().showPetById().path(path -> path.petId("1234"));

    /* Using the native API, create a new http request. It will use our templated request for
     * default values, but we can override any part of the request, like the query string.
     */
    var request =
        HttpRequest.newBuilder(operation.httpRequest(), (k, v) -> true)
            // We can use baseUri(), pathString(), and queryString() from the operation to override
            // templated URIs. This lets us work around incomplete specifications until they are
            // fixed.
            .uri(URI.create(operation.baseUri() + operation.pathString() + "?foo=foo&bar=bar"))
            .build();

    /* Dispatch the customized request, taking advantage of the same sendSync behavior we normally
     * would.
     */
    var response = operation.sendSync(request);
    if (response instanceof ShowPetById200 ok) {
      assertEquals(Pet.newBuilder().setId(1234L).setName("Reginald").build(), ok.body());
    } else {
      fail("Expected a 200 response");
    }
  }
}
