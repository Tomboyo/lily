package io.github.tomboyo.lily.http.model;

import io.github.tomboyo.lily.http.Lily;
import io.github.tomboyo.lily.http.Result;
import java.net.http.HttpResponse;

@FunctionalInterface
public interface ResponseReader<T> {
  Result<T, Lily.SendSyncError.ReadResponseError> readResponse(HttpResponse<byte[]> httpResponse);
}
