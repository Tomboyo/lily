package io.github.tomboyo.lily.compiler;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public class CompilerSupport {

  private static final Path GENERATED_SOURCES = Path.of("target", "sources-under-test");
  private static final Path TEST_CLASSES = Path.of("target", "test-classes");

  private static final AtomicInteger SERIAL = new AtomicInteger(0);

  public static String uniquePackageName() {
    return "gen" + SERIAL.incrementAndGet();
  }

  /**
   * Generate and compile source code from the given OAS document string.
   *
   * @param rootPackageName The package within which to generate code.
   * @param oas The OpenAPI specification document contents as a string.
   * @throws OasParseException If this fails for any reason.
   */
  public static void compileOas(String rootPackageName, String oas) throws OasParseException {
    var generatedSourcePaths = LilyCompiler.compile(oas, GENERATED_SOURCES, rootPackageName, true);
    compileJavaSources(TEST_CLASSES, generatedSourcePaths);
  }

  @Deprecated(forRemoval = true)
  public static String compileOas(String oas) throws OasParseException {
    var packageName = uniquePackageName();
    compileOas(packageName, oas);
    return packageName;
  }

  @Deprecated(forRemoval = true)
  public static Object evaluate(String source) {
    return evaluate(uniquePackageName(), Object.class, source);
  }

  @Deprecated(forRemoval = true)
  public static <T> T evaluate(String source, Class<T> type) {
    return evaluate(uniquePackageName(), type, source);
  }

  /**
   * Shorthand for {@code evaluate(rootPackageName, source, Object.class)}; see {@link
   * #evaluate(String, Class, String)}.
   */
  public static Object evaluate(String packageName, String source) {
    return evaluate(packageName, Object.class, source);
  }

  /**
   * Compile and evaluate the given code block.
   *
   * <p>The code block must end with a return statement and constitute a valid {@code public static
   * T} function body.
   *
   * @param packageName The package within which to compile the given code.
   * @param source The source code block to evaluate.
   * @param type The Class of the returned object.
   * @param <T> The type of the returned object.
   * @return An instance of T returned by compiling and invoking the given source code.
   */
  public static <T> T evaluate(String packageName, Class<T> type, String source) {
    var className = "Wrapper" + SERIAL.incrementAndGet();
    var wrapperSource =
        """
        package %s;
        public class %s {
          public static Object eval() throws Exception {
              %s
          }
        }
        """
            .formatted(packageName, className, source);
    var wrapperDir = GENERATED_SOURCES.resolve(Path.of(".", packageName.split("\\.")));
    var file = wrapperDir.resolve(className + ".java");

    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, wrapperSource, CREATE, WRITE, TRUNCATE_EXISTING);
      compileJavaSources(TEST_CLASSES, List.of(file));
      return type.cast(Class.forName(packageName + "." + className).getMethod("eval").invoke(null));
    } catch (Exception e) {
      throw new RuntimeException("Failed to evaluate source code", e);
    }
  }

  public static void deleteGeneratedSourcesInPackage(String packageName) throws IOException {
    var directory =
        Arrays.stream(packageName.split("\\."))
            .reduce(GENERATED_SOURCES, Path::resolve, Path::resolve);
    deleteAllInDirectoryRecursively(directory);
  }

  @Deprecated(forRemoval = true)
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

    Files.delete(dir);
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
            List.of(
                "-d", classesDir.toString(), "-classpath", System.getProperty("java.class.path")),
            null,
            compilationUnits)
        .call();

    listener.getDiagnostics().stream()
        .findFirst()
        .ifPresent(
            (it) -> {
              var source = readAll(it.getSource());
              var affectedLines =
                  getLinesAround(source, it.getStartPosition(), it.getEndPosition());
              throw new RuntimeException(
                  "Compilation error in %s:%n%n%s%n```%n%s%n```%n"
                      .formatted(it.getSource(), it.getMessage(null), affectedLines));
            });
  }

  private static String readAll(JavaFileObject o) {
    try (var raw = o.openInputStream();
        var reader = new InputStreamReader(raw);
        var buffered = new BufferedReader(reader)) {
      return buffered.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String getLinesAround(String source, long fromLong, long toLong) {
    int from = (int) fromLong;
    int to = (int) toLong;

    // Move pointers to the beginning and end of their respective lines if they aren't already there
    // index may be -1 if absent, so we Max to 0.
    from = Math.max(0, source.lastIndexOf('\n', from));
    var index = source.indexOf('\n', to);
    if (index != -1) {
      to = index;
    }

    return source.substring(from, to).trim();
  }
}
