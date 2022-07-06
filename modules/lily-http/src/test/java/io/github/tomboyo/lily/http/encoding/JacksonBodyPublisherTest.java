package io.github.tomboyo.lily.http.encoding;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.JacksonBodyPublisher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

@WireMockTest
public class JacksonBodyPublisherTest {

  private final HttpClient client = HttpClient.newBuilder().build();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void test(WireMockRuntimeInfo info) throws Exception {
    stubFor(
        any(anyUrl())
            .withRequestBody(
                equalToJson(
                    """
                  {
                    "key": "value"
                  }"""))
            .willReturn(WireMock.ok()));

    var response =
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(info.getHttpBaseUrl()))
                .POST(JacksonBodyPublisher.of(objectMapper, Map.of("key", "value")))
                .build(),
            HttpResponse.BodyHandlers.discarding());

    assertEquals(200, response.statusCode());
  }
}
