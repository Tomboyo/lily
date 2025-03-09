package io.github.tomboyo.lily.compiler.cli;

import io.github.tomboyo.lily.compiler.LilyCompiler;
import io.github.tomboyo.lily.compiler.OasParseException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "compile",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    description = "Compile an OpenAPI schema to java source code.")
public class CompileCommand implements Callable<Integer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompileCommand.class);

  @Option(names = "--source", description = "Path or URL to OAS schema document.")
  private String source;

  @Option(names = "--output-dir", description = "Base directory in which to output sources.")
  private Path output;

  @Option(names = "--base-package", description = "Package name under which to generates sources.")
  private String basePackage;

  @Override
  public Integer call() {
    try {
      LilyCompiler.compile(source, output, basePackage);
      return 0;
    } catch (OasParseException e) {
      LOGGER.error("Failed to parse OAS document", e);
      return 1;
    } catch (RuntimeException e) {
      LOGGER.error("Unexpected error during compilation", e);
      return 1;
    }
  }
}
