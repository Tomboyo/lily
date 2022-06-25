package com.github.tomboyo.lily.compiler;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.github.tomboyo.lily.compiler.cg.AstToJava;
import com.github.tomboyo.lily.compiler.cg.Source;
import com.github.tomboyo.lily.compiler.icg.AstGenerator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class LilyCompiler {

  /**
   * Generate java source code from an OpenAPI V3 YAML specification file.
   *
   * @param oasPath The location of the OpenAPI YAML specification to compile.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If compilation fails for any reason.
   */
  public static Set<Path> compile(Path oasPath, Path outputDir, String basePackage)
      throws OasParseException {
    var openAPI =
        requireValidV3OpenAPI(new OpenAPIParser().readLocation(oasPath.toString(), null, null));
    return compile(openAPI, outputDir, basePackage);
  }

  /**
   * Generate java source code form an OpenAPI V3 YAML specification string.
   *
   * @param oasContent A string representing an OpenAPI V3 YAML specification.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If compilation fails for any reason.
   */
  public static Set<Path> compile(String oasContent, Path outputDir, String basePackage)
      throws OasParseException {
    var openAPI = requireValidV3OpenAPI(new OpenAPIParser().readContents(oasContent, null, null));
    return compile(openAPI, outputDir, basePackage);
  }

  private static Set<Path> compile(OpenAPI openAPI, Path outputDir, String basePackage) {
    return AstGenerator.evaluate(basePackage, openAPI)
        .map(AstToJava::renderAst)
        .map(source -> persistSource(outputDir, source))
        .collect(Collectors.toSet());
  }

  private static OpenAPI requireValidV3OpenAPI(SwaggerParseResult parseResult)
      throws OasParseException {
    var messages = parseResult.getMessages();
    if (messages != null && !messages.isEmpty()) {
      var message = String.join("\n", parseResult.getMessages());
      throw new OasParseException("Failed to parse OAS document:\n" + message);
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

  private static Path persistSource(Path outputDirectory, Source rendering) {
    var destination = outputDirectory.resolve(rendering.relativePath());
    try {
      Files.createDirectories(destination.getParent());
      return Files.writeString(destination, rendering.contents(), CREATE, WRITE, TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to write source file to path '" + destination + "'", e);
    }
  }
}
