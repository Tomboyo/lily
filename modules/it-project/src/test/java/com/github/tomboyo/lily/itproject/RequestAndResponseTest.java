package com.github.tomboyo.lily.itproject;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomboyo.lily.itproject.WiremockSupport.newBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.RequestAndResponseRequest;
import com.example.RequestAndResponseResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomboyo.lily.http.JacksonBodyHandler;
import com.github.tomboyo.lily.http.JacksonBodyPublisher;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * These test cases demonstrate the current level of support for creating requests with complex
 * request and response objects.
 */
@WireMockTest
public class RequestAndResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient client = HttpClient.newBuilder().build();

  @BeforeEach
  public void setup() {
    stubFor(
        any(anyUrl())
            .withRequestBody(
                equalToJson("""
            {
              "foo": "request"
            }"""))
            .willReturn(WireMock.ok("""
        {
          "foo": "response"
        }""")));
  }

  // TODO: inline request/response schemas are not supported!
  @Nested
  public class Inline {
    @Test
    public void postRequestAndResponse(WireMockRuntimeInfo info) throws Exception {
      var response =
          client.send(
              newBuilder(info, "requestAndResponse/")
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          """
            {
              "foo": "request"
            }"""))
                  .build(),
              new JacksonBodyHandler<>(new ObjectMapper(), new TypeReference<JsonNode>() {}));

      assertEquals(200, response.statusCode());
      assertEquals("response", response.body().get().get("foo").asText());
    }
  }

  // Schemas defined in components/schemas are supported. JacksonBodyPublisher and
  // JacksonBodyHandler provide convenience for serializing and deserializing complex types using
  // the java.net.http API.
  @Nested
  public class Ref {
    @Test
    public void postRequestAndResponse(WireMockRuntimeInfo info) throws Exception {
      var response =
          client.send(
              newBuilder(info, "requestAndResponse/")
                  .POST(
                      JacksonBodyPublisher.of(
                          objectMapper, new RequestAndResponseRequest("request")))
                  .build(),
              new JacksonBodyHandler<>(
                  new ObjectMapper(), new TypeReference<RequestAndResponseResponse>() {}));

      assertEquals(200, response.statusCode());
      assertEquals("response", response.body().get().foo());
    }
  }
}
