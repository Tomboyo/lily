package com.github.tomboyo.lily.itproject;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomboyo.lily.http.encoding.Encoding.simple;
import static com.github.tomboyo.lily.itproject.WiremockSupport.newBuilder;
import static java.net.http.HttpResponse.BodyHandlers.discarding;

import com.example.RGB;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.Test;

@WireMockTest
public class ParametersTest {

  private final HttpClient client = HttpClient.newBuilder().build();

  /** See oas.yaml paths.simpleParameterStyle */
  @Test
  public void simpleParameterStyle(WireMockRuntimeInfo info) throws Exception {
    client.send(
        newBuilder(info, "/simpleParameterStyle/" + simple(new RGB(100, 200, 255)))
            .header("MyHeader", simple(List.of(1, 2, 3)))
            .build(),
        discarding());

    verify(
        anyRequestedFor(urlPathEqualTo("/simpleParameterStyle/r,100,g,200,b,255"))
            .withHeader("MyHeader", equalTo("1,2,3")));
  }
}
