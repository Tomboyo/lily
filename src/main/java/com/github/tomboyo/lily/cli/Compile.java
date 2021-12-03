package com.github.tomboyo.lily.cli;

import com.github.tomboyo.lily.ast.OasSchemaToAst;
import io.swagger.parser.OpenAPIParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "compile",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "Compile an OpenAPI schema to java source code.")
public class Compile implements Callable<Void> {
  @Option(names = "--source", description = "Path to OAS schema document.")
  private Path source;

  @Option(names = "--output-dir", description = "Base directory in which to output sources.")
  private Path output;

  @Option(names = "--base-package", description = "Package name under which to generates sources.")
  private String basePackage;

  @Override
  public Void call() throws Exception {
    var parseResult = new OpenAPIParser().readLocation(source.toString(), null, null);

    if (!parseResult.getMessages().isEmpty()) {
      parseResult.getMessages().forEach(System.err::println);
      return null;
    }

    var openApi = parseResult.getOpenAPI();
    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      System.err.println("Only OAS version 3.x is supported. Got " + version);
    }

    // TODO: render source instead of printing.
    openApi.getComponents().getSchemas().entrySet().stream()
        .flatMap(entry -> OasSchemaToAst.generateAst(basePackage, entry.getKey(), entry.getValue()))
        .map(System.err::println);

    return null;
  }
}
