package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSourcesInPackage;
import static io.github.tomboyo.lily.compiler.CompilerSupport.uniquePackageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Provides code generation support. All tests within the annotated class shared a package for
 * generated sources.
 */
public class LilyExtension implements AfterAllCallback, ParameterResolver {

  private final LilyTestSupport instance = new LilyTestSupport();

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    instance.deleteGeneratedSources();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getAnnotatedType().getType() == LilyTestSupport.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return instance;
  }

  public static class LilyTestSupport {
    private final String packageName = uniquePackageName();
    private boolean hasGeneratedCode = false;

    /**
     * Generates and compiles java sources for the given OpenAPI specification.
     *
     * @param specification An OpenAPI document as a string.
     * @see CompilerSupport#compileOas(String, String)
     */
    public void compileOas(String specification) {
      if (hasGeneratedCode) {
        throw new IllegalStateException(
            """
            This support instance has already generated code within the current generated sources
            package. Classes generated previously may coincidentally have the same name as classes
            that are expected from this invocation and used in subsequent tests. We are refusing to
            generate code to avoid such false negatives.

            How to resolve: Ensure that whenever you need to compile an OAS document, you do so
            within a dedicated `@Nested` class with the LilyExtension. Do not only annotate the
            outer class.
            """);
      }
      hasGeneratedCode = true;

      try {
        CompilerSupport.compileOas(packageName, specification);
      } catch (OasParseException e) {
        throw new RuntimeException(e);
      }
    }

    /** See {@link #evaluate(String, Class, String...)}. */
    public Object evaluate(String source, String... kvs) {
      return evaluate(source, Object.class, kvs);
    }

    /**
     * Evaluate the given source code, which MUST have a return statement. Source code is first
     * interpolated such that mustache-like {@code {{key}}} expressions are replaced with their
     * corresponding values, as determined by the kvs parameter. Note that the {@code {{package}}}
     * key always evaluates to the package name within which all code is evaluated, which may be
     * used to construct the FQN of types generated with {@link #compileOas(String)}.
     *
     * @param source The source code as a string to evaluate.
     * @param returnType The expected return type
     * @param kvs A sequence of key and value pair strings to interpolate into the source code.
     * @param <T> The expected return type.
     * @return The result of evaluating the code.
     * @see CompilerSupport#evaluate(String, Class, String)
     */
    public <T> T evaluate(String source, Class<T> returnType, String... kvs) {
      if (kvs.length % 2 != 0) {
        throw new IllegalArgumentException("Must be an even number of key-value pairs");
      }

      source = source.replaceAll("\\{\\{package}}", packageName);

      for (var i = 0; i < kvs.length / 2; i += 2) {
        var key = kvs[i];
        var value = kvs[i + 1];
        source = source.replaceAll("\\{\\{" + key + "}}", value);
      }

      return CompilerSupport.evaluate(packageName, returnType, source);
    }

    private void deleteGeneratedSources() {
      try {
        deleteGeneratedSourcesInPackage(packageName);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
