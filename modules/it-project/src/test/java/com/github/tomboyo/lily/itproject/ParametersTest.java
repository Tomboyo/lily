package com.github.tomboyo.lily.itproject;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomboyo.lily.itproject.WiremockSupport.newBuilder;
import static java.net.http.HttpResponse.BodyHandlers.discarding;

import com.example.RGB;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@WireMockTest
public class ParametersTest {

  private final HttpClient client = HttpClient.newBuilder().build();

  // TODO: inline parameter schema generation is not supported.
  @Nested
  public class SchemaGeneration {
    @Test
    @Disabled
    public void pathInline() {}

    @Test
    @Disabled
    public void queryInline() {}

    @Test
    @Disabled
    public void headerInline() {}

    @Test
    @Disabled
    public void cookieInline() {}
  }

  @Nested
  public class InPath {

    @Nested
    public class Simple {

      @Test
      public void object(WireMockRuntimeInfo info) throws Exception {
        client.send(
            newBuilder(
                    info,
                    "/parametersSimpleObjectRef/" + new RGB(100, 200, 255).simplePathEncoding())
                .build(),
            discarding());

        verify(anyRequestedFor(urlPathEqualTo("/parametersSimpleObjectRef/r,100,g,200,b,255")));
      }
    }
  }
}
