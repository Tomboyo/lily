package io.github.tomboyo.lily.example;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.http.Api;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

@WireMockTest
public class ApiTest {
  @Test
  void test(WireMockRuntimeInfo info) throws InterruptedException {
    var actual = Api.sendSync(
            HttpClient.newBuilder().build(),
            info.getHttpBaseUrl(),
            // TODO: remove this somehow -- make the requestWriter part of the Operation?
            JsonMapper.builder().build(),
            Api.Operations.getPet(),
            template -> template
                .withPathParameters(p -> p.withId("mock-id"))
                .withQueryParameters(p -> p
                    .withFoo("foo!")
                    .withBar("bar!")
                    .withBaz("baz!")))
        .unwrap(
            ok -> switch (ok) {
              case Api.GetPet200 ignored -> 200;
              case Api.GetPet404 ignored -> 404;
            },
            error -> {
              throw new RuntimeException("Failed to get pet", error.exception());
            });
  }
}
