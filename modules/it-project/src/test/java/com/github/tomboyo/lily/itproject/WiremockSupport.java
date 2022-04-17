package com.github.tomboyo.lily.itproject;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import java.net.URI;
import java.net.http.HttpRequest;

public class WiremockSupport {
  private WiremockSupport() {}

  public static HttpRequest.Builder newBuilder(WireMockRuntimeInfo info) {
    return newBuilder(info, "");
  }

  public static HttpRequest.Builder newBuilder(WireMockRuntimeInfo info, String path) {
    return HttpRequest.newBuilder(URI.create(info.getHttpBaseUrl() + path));
  }
}
