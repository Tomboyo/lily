package io.github.tomboyo.lily.compiler;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class CompilerSupport {

  private static final Path GENERATED_SOURCES = Path.of("target", "sources-under-test");
  private static final Path TEST_CLASSES = Path.of("target", "test-classes");

  private static final AtomicInteger PACKAGE_SERIAL = new AtomicInteger(0);

  public static String compileOas(String oas) throws OasParseException {
    var packageName = uniquePackageName();
    var generatedSourcePaths = LilyCompiler.compile(oas, GENERATED_SOURCES, packageName, true);
    compileJavaSources(TEST_CLASSES, generatedSourcePaths);
    return packageName;
  }

  /** Shorthand for {@code evaluate(source, Object.class)}; see {@link #evaluate(String, Class)}. */
  public static Object evaluate(String source) {
    return evaluate(source, Object.class);
  }

  /**
   * Compile and evaluate the given code block.
   *
   * <p>The code block must end with a return statement and constitute a valid {@code public static
   * T} function body.
   *
   * @param source The source code block to evaluate.
   * @param type The Class of the returned object.
   * @param <T> The type of the returned object.
   * @return An instance of T returned by compiling and invoking the given source code.
   */
  public static <T> T evaluate(String source, Class<T> type) {
    var packageName = uniquePackageName();
    var wrapperSource =
        """
        package %s;
        public class Wrapper {
          public static Object eval() {
              %s
          }
        }
        """
            .formatted(packageName, source);

    try {
      var file =
          GENERATED_SOURCES.resolve(Path.of(".", packageName.split("\\."))).resolve("Wrapper.java");
      Files.createDirectories(file.getParent());
      Files.writeString(file, wrapperSource, CREATE, WRITE, TRUNCATE_EXISTING);
      compileJavaSources(TEST_CLASSES, List.of(file));
      return type.cast(Class.forName(packageName + ".Wrapper").getMethod("eval").invoke(null));
    } catch (Exception e) {
      throw new RuntimeException("Failed to evaluate source code", e);
    }
  }

  private static String uniquePackageName() {
    return "gen.p" + PACKAGE_SERIAL.incrementAndGet();
  }

  /** Delete all generated test sources and their compiled classes */
  public static void deleteGeneratedSources() throws IOException {
    deleteAllInDirectoryRecursively(GENERATED_SOURCES);
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

  private static void compileJavaSources(Path classesDir, Collection<Path> sourcePaths) {
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