package com.github.tomboyo.lily.cli;

import com.github.tomboyo.lily.ast.OasSchemaToAst;
import com.github.tomboyo.lily.render.AstToJava;
import com.github.tomboyo.lily.render.Source;
import io.swagger.parser.OpenAPIParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

@Command(
    name = "compile",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "Compile an OpenAPI schema to java source code.")
public class Compile implements Callable<Integer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Compile.class);

  @Option(names = "--source", description = "Path to OAS schema document.")
  private Path source;

  @Option(names = "--output-dir", description = "Base directory in which to output sources.")
  private Path output;

  @Option(names = "--base-package", description = "Package name under which to generates sources.")
  private String basePackage;

  @Override
  public Integer call() throws Exception {
    var parseResult = new OpenAPIParser().readLocation(source.toString(), null, null);

    if (!parseResult.getMessages().isEmpty()) {
      parseResult.getMessages().forEach(System.err::println);
      return 1;
    }

    var openApi = parseResult.getOpenAPI();
    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      System.err.println("Only OAS version 3.x is supported. Got " + version);
      return 1;
    }

    // TODO: render source instead of printing.
    OasSchemaToAst.generateAst(basePackage, openApi.getComponents())
        .map(AstToJava::renderAst)
        .peek(source -> LOGGER.info("Writing source file: '{}'", source.relativePath()))
        .forEach(source -> persistSource(output, source));

    return 0;
  }

  private static void persistSource(Path outputDirectory, Source source) {
    var destination = outputDirectory.resolve(source.relativePath());
    try {
      Files.createDirectories(destination.getParent());
      Files.writeString(destination, source.contents(), StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to write source file to path '" + destination + "'", e);
    }
  }
}
