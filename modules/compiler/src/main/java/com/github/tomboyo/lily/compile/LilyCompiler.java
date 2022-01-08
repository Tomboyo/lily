package com.github.tomboyo.lily.compile;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.github.tomboyo.lily.ast.OasSchemaToAst;
import com.github.tomboyo.lily.render.AstToJava;
import com.github.tomboyo.lily.render.Source;
import io.swagger.parser.OpenAPIParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LilyCompiler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LilyCompiler.class);

  public static void compile(String source, Path output, String basePackage)
      throws OasParseException {
    var parseResult = new OpenAPIParser().readLocation(source, null, null);

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

    OasSchemaToAst.generateAst(basePackage, openApi.getComponents())
        .map(AstToJava::renderAst)
        .forEach(rendering -> persistSource(output, rendering));
  }

  private static void persistSource(Path outputDirectory, Source rendering) {
    var destination = outputDirectory.resolve(rendering.relativePath());
    try {
      Files.createDirectories(destination.getParent());
      Files.writeString(destination, rendering.contents(), CREATE, WRITE, TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to write source file to path '" + destination + "'", e);
    }
  }
}
