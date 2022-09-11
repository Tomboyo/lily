package io.github.tomboyo.lily.compiler;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.cg.AstToJava;
import io.github.tomboyo.lily.compiler.cg.Source;
import io.github.tomboyo.lily.compiler.icg.AstGenerator;
import io.github.tomboyo.lily.compiler.oas.OasReader;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class LilyCompiler {

  /**
   * Generate java source code from an OpenAPI V3 YAML specification file.
   *
   * @param oasUri The URI of the OpenAPI YAML specification to compile.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @param allowWarnings Log but otherwise ignore OAS validation/parsing errors if possible.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static Set<Path> compile(
      URI oasUri, Path outputDir, String basePackage, boolean allowWarnings)
      throws OasParseException {
    var openAPI = OasReader.fromUri(oasUri, allowWarnings);
    return compile(openAPI, outputDir, basePackage);
  }

  /**
   * Generate java source code form an OpenAPI V3 YAML specification string.
   *
   * @param oasContent A string representing an OpenAPI V3 YAML specification.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @param allowWarnings Log but otherwise ignore OAS validation/parsing errors if possible.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static Set<Path> compile(
      String oasContent, Path outputDir, String basePackage, boolean allowWarnings)
      throws OasParseException {
    var openAPI = OasReader.fromString(oasContent, allowWarnings);
    return compile(openAPI, outputDir, basePackage);
  }

  private static Set<Path> compile(OpenAPI openAPI, Path outputDir, String basePackage) {
    return AstGenerator.evaluate(PackageName.of(basePackage), openAPI)
        .map(AstToJava::renderAst)
        .map(source -> persistSource(outputDir, source))
        .collect(Collectors.toSet());
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
