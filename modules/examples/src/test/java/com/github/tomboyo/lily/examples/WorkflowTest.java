package com.github.tomboyo.lily.examples;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.PostFooBarBody;
import com.example.PostFooBarResponse;
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
        post("/foo/bar/")
            .withRequestBody(equalToJson("{\"foo\": \"foo\", \"bar\": \"bar\"}"))
            .willReturn(ok("""
        {
          "baz": "baz"
        }""")));

    var client = HttpClient.newBuilder().build();
    var response =
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(info.getHttpBaseUrl() + "/foo/bar/"))
                .POST(
                    HttpRequest.BodyPublishers.ofByteArray(
                        objectMapper.writeValueAsBytes(new PostFooBarBody("foo", "bar"))))
                .build(),
            new JacksonBodyHandler<>(
                new ObjectMapper(), new TypeReference<PostFooBarResponse>() {}));

    assertEquals(200, response.statusCode());
    assertEquals("baz", response.body().get().baz());
  }
}
