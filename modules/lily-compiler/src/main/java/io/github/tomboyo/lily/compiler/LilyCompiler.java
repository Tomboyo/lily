package io.github.tomboyo.lily.compiler;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.cg.CodeGen;
import io.github.tomboyo.lily.compiler.cg.Source;
import io.github.tomboyo.lily.compiler.icg.AstGenerator;
import io.github.tomboyo.lily.compiler.oas.OasReader;
import io.github.tomboyo.lily.compiler.oas.model.OpenApi;
import io.github.tomboyo.lily.compiler.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class LilyCompiler {

  /**
   * Generate java source code from an OpenAPI V3 YAML specification file.
   *
   * @param url The URI of the OpenAPI YAML specification to compile.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static Map<String, Path> compile(URL url, Path outputDir, String basePackage)
      throws OasParseException {
    var openAPI = OasReader.fromUrl(url);
    return compile(openAPI, outputDir, basePackage);
  }

  /**
   * Generate java source code form an OpenAPI V3 YAML specification string.
   *
   * @param oasContent A string representing an OpenAPI V3 YAML specification.
   * @param outputDir The parent directory to save java generated source code files.
   * @param basePackage The name of the base package for all generated java source files.
   * @return The set of Path objects for each generated file.
   * @throws OasParseException If reading the document fails for any reason.
   */
  public static Map<String, Path> compile(String oasContent, Path outputDir, String basePackage)
      throws OasParseException {
    var openAPI = OasReader.fromString(oasContent);
    return compile(openAPI, outputDir, basePackage);
  }

  private static Map<String, Path> compile(OpenApi openApi, Path outputDir, String basePackage) {
    return AstGenerator.evaluate(PackageName.of(basePackage), openApi)
        .map(CodeGen::renderAst)
        .map(source -> new Pair<>(source.fqn(), persistSource(outputDir, source)))
        .collect(Collectors.toMap(Pair::left, Pair::right));
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
