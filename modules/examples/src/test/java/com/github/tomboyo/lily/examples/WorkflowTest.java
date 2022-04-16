package com.github.tomboyo.lily.examples;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.PostFooBody;
import com.example.PostFooResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomboyo.lily.http.JacksonBodyHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

/**
 * These test cases simulate a user taking advantage of Lily to help them make HTTP requests. We
 * simulate a service using wiremock and make HTTP requests against that service using
 * java.net.http. To start, almost every part of the http client is hand-written. As Lily becomes
 * more useful, more and more of the HTTP request will use generated support.
 */
@WireMockTest
public class WorkflowTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void test(WireMockRuntimeInfo info) throws Exception {
    stubFor(
        any(urlPathEqualTo("/foo/x"))
            .withRequestBody(equalToJson("{\"bar\": \"bar\"}"))
            .withHeader("My-Header", equalTo("myHeader"))
            .withQueryParam("queryOption", equalTo("true"))
            .withCookie("MyCookie", equalTo("MyCookie"))
            .willReturn(ok("""
        {
          "baz": "baz"
        }""")));

    var client = HttpClient.newBuilder().build();
    var response =
        client.send(
            HttpRequest.newBuilder()
                // "simple" path parameter style (default),
                // "form" query parameter style (default)
                .uri(URI.create(info.getHttpBaseUrl() + "/foo/x?queryOption=true"))
                // "simple" header parameter style (default)
                .header("My-Header", "myHeader")
                .header("Cookie", "MyCookie=MyCookie")
                .POST(
                    HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new PostFooBody("bar"))))
                .build(),
            new JacksonBodyHandler<>(new ObjectMapper(), new TypeReference<PostFooResponse>() {}));

    assertEquals(200, response.statusCode());
    assertEquals("baz", response.body().get().baz());
  }
}
