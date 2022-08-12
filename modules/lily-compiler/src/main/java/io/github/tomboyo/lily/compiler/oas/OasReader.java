package io.github.tomboyo.lily.compiler.oas;

import io.github.tomboyo.lily.compiler.OasParseException;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

/**
 * Reads source OpenAPI Specification representations into OpenAPI object format.
 */
public class OasReader {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(OasReader.class);

  /**
   * Read an OpenAPI object from a String source.
   *
   * @param oasContent A String containing an OpenAPI V3 YAML specification.
   * @param allowWarnings Log but otherwise ignore OAS validation/parsing errors if possible.
   * @return An OpenAPI object representation of the source document.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static OpenAPI fromString(String oasContent, boolean allowWarnings) throws OasParseException {
    var parseResult = new OpenAPIParser().readContents(oasContent, null, null);
    return requireValidV3OpenAPI(parseResult, allowWarnings);
  }

  /**
   * Read an OpenAPI object from a source located by a URI.
   *
   * @param oasUri The URI of an OpenAPI YAML specification resource.
   * @param allowWarnings Log but otherwise ignore OAS validation/parsing errors if possible.
   * @return An OpenAPI object representation of the source document.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static OpenAPI fromUri(URI oasUri, boolean allowWarnings) throws OasParseException {
    var parseResult = new OpenAPIParser().readLocation(oasUri.toString(), null, null);
    return requireValidV3OpenAPI(parseResult, allowWarnings);
  }
  
  private static OpenAPI requireValidV3OpenAPI(
      SwaggerParseResult parseResult, boolean allowWarnings) throws OasParseException {
    var warnings = requireNonNullElse(parseResult.getMessages(), List.of());

    warnings.forEach(warn -> LOGGER.warn("OpenAPI parse error: {}", warn));

    if (!warnings.isEmpty() && !allowWarnings) {
      throw new OasParseException("OAS contains validation errors (see preceding errors)");
    }

    var openApi = parseResult.getOpenAPI();
    if (openApi == null) {
      throw new OasParseException("Failed to parse OpenAPI document (see preceding errors)");
    }

    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      throw new OasParseException("OAS version 3 or higher required. Got version=" + version);
    }

    return openApi;
  }
}
