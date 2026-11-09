package io.github.tomboyo.lily.http;

import static java.util.Objects.requireNonNull;

import java.net.http.HttpClient;

public class OptionsBuilder {
  private HttpClient httpClient;
  private String baseUrl;

  public OptionsBuilder setHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  public OptionsBuilder setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  public Options build() {
    requireNonNull(httpClient, "httpClient must not be null");
    requireNonNull(baseUrl, "baseUrl must not be null");
    return new Options(httpClient, baseUrl);
  }
}
