package com.github.tomboyo.lily.compiler.icg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomboyo.lily.compiler.LilyCompiler;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests the APIs generated from OAS paths. */
public class PathsTest {

  private static final Path sources = Path.of("target", "generated-test-sources");
  private static final Path classes = Path.of("target", "test-classes");

  @BeforeAll
  public static void beforeAll() throws Exception {
    var generatedSources =
        LilyCompiler.compile(
            """
        openapi: 3.0.2
        info:
          title: MultipleTags
          description: "An operation with multiple tags"
          version: 0.1.0
        paths:
          /pets/:
            get:
              operationId: getPets
              tags:
                - dogs
                - cats
              responses:
                "204":
                  description: OK
        """,
            sources,
            "com.example.tmptest");

    compileJava(generatedSources);
  }

  @Test
  public void foo() throws Exception {
    assertEquals(
        "com.example.tmptest.DogsOperations",
        Class.forName("com.example.tmptest.Api")
            .getMethod("dogsOperations")
            .getReturnType()
            .getName(),
        "api.dogsOperations() returns DogsOperations");

    assertEquals(
        "com.example.tmptest.CatsOperations",
        Class.forName("com.example.tmptest.Api")
            .getMethod("catsOperations")
            .getReturnType()
            .getName(),
        "api.catsOperations() returns CatsOperations");

    assertEquals(
        "com.example.tmptest.GetPetsOperation",
        Class.forName("com.example.tmptest.DogsOperations")
            .getMethod("getPets")
            .getReturnType()
            .getName(),
        "api.dogsOperations().getPets() returns the GetPetsOperation");

    assertEquals(
        "com.example.tmptest.GetPetsOperation",
        Class.forName("com.example.tmptest.CatsOperations")
            .getMethod("getPets")
            .getReturnType()
            .getName(),
        "api.catsOperations().getPets() returns the GetPetsOperations");
  }

  private static void compileJava(Collection<Path> sources) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var listener = new DiagnosticCollector<JavaFileObject>();
    var fileManager = compiler.getStandardFileManager(listener, null, null);
    var compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);

    compiler
        .getTask(
            null, fileManager, listener, List.of("-d", classes.toString()), null, compilationUnits)
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
