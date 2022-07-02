package com.github.tomboyo.lily.compiler.icg;

import com.github.tomboyo.lily.compiler.LilyCompiler;
import com.github.tomboyo.lily.compiler.OasParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class CompilerSupport {

  private static final Path GENERATED_TEST_SOURCES = Path.of("target", "generated-test-sources");
  private static final Path TEST_CLASSES = Path.of("target", "test-classes");

  public static void generateAndCompile(String basePackage, String oasContents)
      throws OasParseException {
    var sourcePaths = LilyCompiler.compile(oasContents, GENERATED_TEST_SOURCES, basePackage, true);
    compileJava(TEST_CLASSES, sourcePaths);
  }

  /** Delete all generated test sources and their compiled classes */
  public static void deleteGeneratedSourcesAndClasses(String basePackage) throws IOException {
    var pathString = basePackage.replaceAll("\\.", "/");
    deleteAllInDirectoryRecursively(TEST_CLASSES.resolve(pathString));
    deleteAllInDirectoryRecursively(GENERATED_TEST_SOURCES.resolve(pathString));
  }

  private static void deleteAllInDirectoryRecursively(Path dir) throws IOException {
    if (!dir.toFile().exists() || !dir.toFile().isDirectory()) {
      return;
    }

    try (var stream = Files.newDirectoryStream(dir)) {
      for (var path : stream) {
        if (path.toFile().isDirectory()) {
          deleteAllInDirectoryRecursively(path);
        } else {
          Files.deleteIfExists(path);
        }
      }
    }
  }

  private static void compileJava(Path classesDir, Collection<Path> sourcePaths) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var listener = new DiagnosticCollector<JavaFileObject>();
    var fileManager = compiler.getStandardFileManager(listener, null, null);
    var compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourcePaths);

    compiler
        .getTask(
            null,
            fileManager,
            listener,
            List.of("-d", classesDir.toString()),
            null,
            compilationUnits)
        .call();

    listener.getDiagnostics().stream()
        .findFirst()
        .ifPresent(
            (it) -> {
              throw new RuntimeException(
                  "Compilation error: code=%s kind=%s pos=%d startPosition=%d endPosition=%d source=%s\n%s%n"
                      .formatted(
                          it.getCode(),
                          it.getKind(),
                          it.getPosition(),
                          it.getStartPosition(),
                          it.getEndPosition(),
                          it.getSource(),
                          it.getMessage(null)));
            });
  }
}
