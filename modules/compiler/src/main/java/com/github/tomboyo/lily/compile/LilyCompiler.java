package com.github.tomboyo.lily.compile;

import com.github.tomboyo.lily.ast.OasSchemaToAst;
import com.github.tomboyo.lily.render.AstToJava;
import com.github.tomboyo.lily.render.Source;
import io.swagger.parser.OpenAPIParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LilyCompiler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LilyCompiler.class);

  public static void compile(
      String source,
      Path output,
      String basePackage
  ) throws OasParseException {
    var parseResult = new OpenAPIParser().readLocation(source, null, null);

    if (!parseResult.getMessages().isEmpty()) {
      var message = String.join("\n", parseResult.getMessages());
      throw new OasParseException("Failed to parse OAS document:\n" + message);
    }

    var openApi = parseResult.getOpenAPI();
    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      throw new OasParseException("OAS version 3 or higher required. Got version=" + version);
    }

    // TODO: render source instead of printing.
    OasSchemaToAst.generateAst(basePackage, openApi.getComponents())
        .map(AstToJava::renderAst)
        .peek(rendering -> LOGGER.info("Writing source file: '{}'", rendering.relativePath()))
        .forEach(rendering -> persistSource(output, rendering));
  }

  private static void persistSource(Path outputDirectory, Source rendering) {
    var destination = outputDirectory.resolve(rendering.relativePath());
    try {
      Files.createDirectories(destination.getParent());
      Files.writeString(destination, rendering.contents(), StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to write source file to path '" + destination + "'", e);
    }
  }
}
