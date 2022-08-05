package io.github.tomboyo.lily.compiler.util;

import java.util.function.Function;

public record Pair<L, R>(L left, R right) {
  public <L2> Pair<L2, R> mapLeft(Function<L, L2> mapper) {
    return new Pair<>(mapper.apply(left), right);
  }

  public <R2> Pair<L, R2> mapRight(Function<R, R2> mapper) {
    return new Pair<>(left, mapper.apply(right));
  }
}
