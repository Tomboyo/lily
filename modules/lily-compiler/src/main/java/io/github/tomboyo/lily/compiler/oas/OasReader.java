package io.github.tomboyo.lily.compiler.oas;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.github.tomboyo.lily.compiler.OasParseException;
import io.github.tomboyo.lily.compiler.oas.jackson.NoneAwareDeserializerModifier;
import io.github.tomboyo.lily.compiler.oas.model.None;
import io.github.tomboyo.lily.compiler.oas.model.OpenApi;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads source OpenAPI Specification representations into OpenAPI object format. */
public class OasReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(OasReader.class);
  private static final ObjectMapper MAPPER = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    var mapper =
        new ObjectMapper(new YAMLFactory())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Ignore malformed YAML entries where possible.
            .addHandler(new ProblemHandler())
            .registerModule(
                new SimpleModule().setDeserializerModifier(new NoneAwareDeserializerModifier()));

    mapper
        .configOverride(Map.class)
        .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY, Nulls.DEFAULT));
    mapper
        .configOverride(List.class)
        .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY, Nulls.DEFAULT));
    mapper
        .configOverride(Set.class)
        .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY, Nulls.DEFAULT));

    return mapper;
  }

  /**
   * Read an OpenAPI object from a String source.
   *
   * @param oasContent A String containing an OpenAPI V3 YAML specification.
   * @return An OpenAPI object representation of the source document.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static OpenApi fromString(String oasContent) throws OasParseException {
    try {
      var openApi = MAPPER.readValue(oasContent, OpenApi.class);
      validate(openApi);
      return openApi;
    } catch (IOException e) {
      throw new OasParseException("Could not load openapi specification", e);
    }
  }

  /**
   * Read an OpenAPI object from a source located by a URI.
   *
   * @param oasUrl The URL of an OpenAPI YAML specification resource.
   * @return An OpenAPI object representation of the source document.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static OpenApi fromUrl(URL url) throws OasParseException {
    try (var is = url.openStream()) {
      var openApi = MAPPER.readValue(is, OpenApi.class);
      validate(openApi);
      return openApi;
    } catch (IOException e) {
      throw new OasParseException("Could not load openapi specification", e);
    }
  }

  private static void validate(OpenApi spec) {
    var version = spec.openapi();
    if (version.isEmpty() || !version.get().startsWith("3.")) {
      LOGGER.warn("Versions 3 and higher are supported. Got: '{}'", version.orElse("null"));
    }
  }

  private static class ProblemHandler extends DeserializationProblemHandler {

    @Override
    public JavaType handleMissingTypeId(
        DeserializationContext context,
        JavaType baseType,
        TypeIdResolver idResolver,
        String failureMsg)
        throws IOException {
      var permitted = baseType.getRawClass().getPermittedSubclasses();
      if (permitted != null && Arrays.asList(permitted).contains(None.class)) {
        return context.constructType(None.class);
      }

      return super.handleMissingTypeId(context, baseType, idResolver, failureMsg);
    }

    @Override
    public Object handleWeirdKey(
        DeserializationContext ctxt, Class<?> rawKeyType, String keyValue, String failureMsg)
        throws IOException {
      return null;
    }

    @Override
    public Object handleWeirdNativeValue(
        DeserializationContext ctxt, JavaType targetType, Object valueToConvert, JsonParser p)
        throws IOException {
      return null;
    }

    @Override
    public Object handleWeirdNumberValue(
        DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg)
        throws IOException {
      return null;
    }

    @Override
    public Object handleWeirdStringValue(
        DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg)
        throws IOException {
      return null;
    }
  }
}
