package io.github.tomboyo.lily.http;

import java.util.function.Function;

// TODO: Should this be moved to a utility package separate from lily-http?
public sealed interface Result<T, E> permits Result.Ok, Result.Error {
  static <T, E> Result<T, E> ok(T value) {
    return new Result.Ok<>(value);
  }

  static <T, E> Result<T, E> error(E value) {
    return new Result.Error<>(value);
  }

  static <T> Result<T, Exception> of(ThrowingSupplier<T> supplier) {
    try {
      return Result.ok(supplier.get());
    } catch (Exception e) {
      return Result.error(e);
    }
  }

  <T2> Result<T2, E> map(Function<T, T2> f);

  <T2, E2 extends E> Result<T2, E> flatMap(Function<T, Result<T2, E2>> f);

  <X> X reduce(Function<T, X> fOk, Function<E, X> fError);

  T unwrap() throws UnwrapRuntimeException;

  <Ex extends Throwable> T unwrap(Function<E, Ex> fError) throws Ex;

  class UnwrapRuntimeException extends RuntimeException {
    public UnwrapRuntimeException(String message) {
      super("Attempted to unwrap a Result.Error: " + message);
    }
  }

  @FunctionalInterface
  interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  record Ok<T, E>(T value) implements Result<T, E> {
    @Override
    public <T2> Result<T2, E> map(Function<T, T2> f) {
      return new Ok<>(f.apply(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T2, E2 extends E> Result<T2, E> flatMap(Function<T, Result<T2, E2>> f) {
      return (Result<T2, E>) f.apply(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E2> Result<T, E2> mapError(Function<E, E2> f) {
      return (Result<T, E2>) this;
    }

    @Override
    public <X> X reduce(Function<T, X> fOk, Function<E, X> fError) {
      return fOk.apply(value);
    }

    @Override
    public <Ex extends Throwable> T unwrap(Function<E, Ex> fError) {
      return value;
    }

    @Override
    public T unwrap() {
      return value;
    }
  }

  record Error<T, E>(E value) implements Result<T, E> {
    @Override
    @SuppressWarnings("unchecked")
    public <T2> Result<T2, E> map(Function<T, T2> f) {
      return (Error<T2, E>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T2, E2 extends E> Result<T2, E> flatMap(Function<T, Result<T2, E2>> f) {
      return (Error<T2, E>) this;
    }

    @Override
    public <E2> Result<T, E2> mapError(Function<E, E2> f) {
      return new Error<>(f.apply(value));
    }

    @Override
    public <X> X reduce(Function<T, X> fOk, Function<E, X> fError) {
      return fError.apply(value);
    }

    @Override
    public <Ex extends Throwable> T unwrap(Function<E, Ex> fError) throws Ex {
      throw fError.apply(value);
    }

    @Override
    public T unwrap() throws UnwrapRuntimeException {
      throw new UnwrapRuntimeException(value.toString());
    }
  }
}
