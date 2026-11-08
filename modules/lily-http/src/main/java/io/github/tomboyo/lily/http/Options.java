package io.github.tomboyo.lily.http;

import java.net.http.HttpClient;

public record Options(HttpClient httpClient, String baseUrl) {
  public static OptionsBuilder builder() {
    return new OptionsBuilder();
  }
}
