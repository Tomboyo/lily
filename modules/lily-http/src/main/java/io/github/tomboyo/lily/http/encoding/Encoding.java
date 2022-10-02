package io.github.tomboyo.lily.http.encoding;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static io.github.tomboyo.lily.http.encoding.Encoding.Modifiers.EXPLODE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;

public class Encoding {

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

  private Encoding() {}

  public enum Modifiers {
    EXPLODE;
  }

  /**
   * Returns an encoder which implements RFC6570 simple-style string expansion, for the {@code
   * {var}} template.
   *
   * @param modifiers
   * @return
   */
  public static Encoder simple(Modifiers... modifiers) {
    if (modifiers.length > 0) {
      throw new UnsupportedOperationException(
          "Only non-exploded simple expansion is currently supported");
    }

    return (String paramName, Object o) -> {
      try {
        return simpleMapper.writer().writeValueAsString(o);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  /**
   * Returns an Encoder which implements RFC6570 form-style string expansion (for templates {@code {?var*}} and {@code {?var}).
   *
   * @param modifiers A list of string expansion modifiers, like EXPLODE.
   * @return An RFC6570 form-style encoder.
   */
  public static Encoder form(Modifiers... modifiers) {
    if (Arrays.asList(modifiers).contains(EXPLODE)) {
      return (String paramName, Object o) -> {
        try {
          return formExplodeMapper.writer().writeValueAsString(Map.of(paramName, o));
        } catch (JsonProcessingException e) {
          throw new UncheckedIOException(e);
        }
      };
    } else {
      throw new UnsupportedOperationException(
          "Only form-style expansion with explode is currently supported");
    }
  }

  /**
   * Returns an Encoder which implements RFC6570 form-style continuation, for templates {@code {&var*}} and {@code {&var}.
   *
   * @param modifiers A list of string expansion modifiers, like EXPLODE.
   * @return An RFC6570 form-style continuation encoder.
   */
  public static Encoder formContinuation(Modifiers... modifiers) {
    if (Arrays.asList(modifiers).contains(EXPLODE)) {
      return (String paramName, Object o) -> {
        try {
          return formContinuationExplodeMapper.writeValueAsString(Map.of(paramName, o));
        } catch (JsonProcessingException e) {
          throw new UncheckedIOException(e);
        }
      };
    } else {
      throw new UnsupportedOperationException(
          "Only form-style expansion with explode is currently supported");
    }
  }
}
