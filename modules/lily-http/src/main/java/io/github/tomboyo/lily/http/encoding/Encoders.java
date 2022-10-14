package io.github.tomboyo.lily.http.encoding;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A collection of Encoder implementations for frequently-used formats, such as RFC6570 simple- and
 * form-style string expansion.
 *
 * @see io.github.tomboyo.lily.http.UriTemplate
 */
public class Encoders {

  private static final ObjectMapper simpleMapper =
      new ObjectMapper(new SimpleFactory())
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private static final ObjectMapper formExplodeMapper =
      new ObjectMapper(new FormExplodeFactory("?"))
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private static final ObjectMapper formContinuationExplodeMapper =
      new ObjectMapper(new FormExplodeFactory("&"))
          .registerModule(new JavaTimeModule())
          .configure(WRITE_DATES_AS_TIMESTAMPS, false);

  private Encoders() {}

  /**
   * Returns an encoder which implements RFC6570 simple-style string expansion.
   *
   * @return The encoder.
   */
  public static Encoder simple() {
    return (String paramName, Object o) -> {
      try {
        return simpleMapper.writer().writeValueAsString(o);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  /**
   * Returns an encoder which uses RFC6570 form-style string expansion with the "explode" modifier for
   * the first parameter it encounters and form-continuation string expansion for the rest, such
   * that a sequence of parameters begins with the '?' query string delimiter and uses the '&'
   * continuation delimiter elsewhere.
   *
   * @return The (stateful!) encoder.
   */
  public static Encoder smartFormExploded() {
    var isFirstParam = new AtomicBoolean(true);
    var form = formExploded();
    var formContinuation = formContinuationExploded();
    return (String paramName, Object o) -> {
      if (isFirstParam.compareAndSet(true, false)) {
        return form.encode(paramName, o);
      } else {
        return formContinuation.encode(paramName, o);
      }
    };
  }

  /**
   * Returns an Encoder which implements RFC6570 form-style string expansion with the "explode"
   * modifier.
   *
   * @return The encoder.
   */
  public static Encoder formExploded() {
    return (String paramName, Object o) -> {
      try {
        return formExplodeMapper.writer().writeValueAsString(Map.of(paramName, o));
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  /**
   * Returns an Encoder which implements RFC6570 form-style continuation with the "explode" modifier.
   *
   * @return The encoder.
   */
  public static Encoder formContinuationExploded() {
    return (String paramName, Object o) -> {
      try {
        return formContinuationExplodeMapper.writeValueAsString(Map.of(paramName, o));
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }
}
