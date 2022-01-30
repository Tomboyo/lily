package com.github.tomboyo.lily.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public record HttpHelper<T>(
    HttpClient client,
    JacksonBodyHandler<T> jacksonBodyHandler,
    HttpRequest.Builder requestBuilder) {

  public CompletableFuture<HttpResponse<T>> sendAsync() {
    return client.sendAsync(requestBuilder.build(), jacksonBodyHandler);
  }

  public HttpResponse<T> send() throws InterruptedException {
    try {
      return client.send(requestBuilder.build(), jacksonBodyHandler);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to complete HTTP request", e);
    }
  }
}
