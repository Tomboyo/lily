package com.github.toboyo.lily.http.encoding;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.JacksonBodyHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
public class JacksonBodyHandlerTest {

  private final HttpClient client = HttpClient.newBuilder().build();

  @Test
  public void test(WireMockRuntimeInfo info) throws Exception {
    stubFor(
        any(anyUrl())
            .willReturn(
                WireMock.ok(
                    """
                  {
                    "key": "value"
                  }""")));

    var response =
        client.send(
            HttpRequest.newBuilder().uri(URI.create(info.getHttpBaseUrl())).build(),
            JacksonBodyHandler.of(new ObjectMapper(), new TypeReference<Map<String, Object>>() {}));

    assertEquals(Map.of("key", "value"), response.body().get());
  }
}
