package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.CompilerSupport.uniquePackageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Generates sources in isolated packages per-class or per-method depending on configuration.
 *
 * <p>Most functions let the user use the string {@code {{package}}} in place of the actual package
 * name of generated types. This lets the extension manage unique package names for the user.
 *
 * <p>In package-per-class mode, sources are generated and compiled once, and the same package is
 * shared by all following tests. Sources are deleted at the end of the class.
 *
 * <p>In package-per-method mode, sources are generated on a test-by-test basis and package names
 * are not shared between tests; tests are completely isolated. Sources are deleted after each test
 * method.
 *
 * <p>If using the {@code @ExtendWith} annotation, it runs in package-per-class mode by default.
 * {@code @RegisterExtension} may be used along with {@link LilyExtension.Builder} to run in either
 * mode.
 *
 * <p>Regardless of mode, all generated classes stay loaded until the JVM exits.
 */
public class LilyExtension
    implements BeforeEachCallback,
        AfterEachCallback,
        BeforeAllCallback,
        AfterAllCallback,
        ParameterResolver {

  enum Mode {
    PACKAGE_PER_CLASS,
    PACKAGE_PER_METHOD
  }

  public static class Builder {

    private Mode mode;

    /**
     * The extension will use the same generated sources package for all tests in the class.
     * Generated files are deleted after the class.
     */
    public Builder packagePerClass() {
      this.mode = Mode.PACKAGE_PER_CLASS;
      return this;
    }

    /**
     * The extension will use a different generated sources package for each test method. Generated
     * files are deleted after each test.
     */
    public Builder packagePerMethod() {
      this.mode = Mode.PACKAGE_PER_METHOD;
      return this;
    }

    public LilyExtension build() {
      return new LilyExtension(mode);
    }
  }

  private static final String PACKAGE = "package";
  private static final String SOURCE_PATHS = "source_paths";

  private final Mode mode;

  public static Builder newBuilder() {
    return new Builder();
  }

  @SuppressWarnings("unused") // instantiated with reflection by ExtendWith annotation
  public LilyExtension() {
    this.mode = Mode.PACKAGE_PER_CLASS;
  }

  private LilyExtension(Mode mode) {
    this.mode = mode;
  }

  @Override
  public void beforeEach(ExtensionContext ctx) {
    if (mode == Mode.PACKAGE_PER_METHOD) {
      setPackage(ctx, uniquePackageName());
    }
  }

  @Override
  public void afterEach(ExtensionContext ctx) throws IOException {
    if (mode == Mode.PACKAGE_PER_METHOD) {
      CompilerSupport.clearPackageFiles(getPackage(ctx));
    }
  }

  @Override
  public void beforeAll(ExtensionContext ctx) {
    if (mode == Mode.PACKAGE_PER_CLASS) {
      setPackage(ctx, uniquePackageName());
    }
  }

  @Override
  public void afterAll(ExtensionContext ctx) throws IOException {
    if (mode == Mode.PACKAGE_PER_CLASS) {
      CompilerSupport.clearPackageFiles(getPackage(ctx));
    }
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getAnnotatedType().getType() == LilyTestSupport.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext ctx)
      throws ParameterResolutionException {
    return new LilyTestSupport(ctx);
  }

  private Store getStore(ExtensionContext ctx) {
    return switch (mode) {
      case PACKAGE_PER_METHOD -> ctx.getStore(
          Namespace.create(getClass(), ctx.getRequiredTestMethod()));
      case PACKAGE_PER_CLASS -> ctx.getStore(
          Namespace.create(getClass(), ctx.getRequiredTestClass()));
    };
  }

  private void setPackage(ExtensionContext ctx, String packageName) {
    getStore(ctx).put(PACKAGE, packageName);
  }

  private String getPackage(ExtensionContext ctx) {
    return getStore(ctx).get(PACKAGE, String.class);
  }

  private void setSourcePaths(ExtensionContext ctx, Map<String, Path> paths) {
    getStore(ctx).put(SOURCE_PATHS, paths);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Path> getSourcePaths(ExtensionContext ctx) {
    return (Map<String, Path>) getStore(ctx).get(SOURCE_PATHS, Map.class);
  }

  public class LilyTestSupport {

    private final ExtensionContext ctx;

    private LilyTestSupport(ExtensionContext ctx) {
      this.ctx = ctx;
    }

    /** Generates and compiled java sources for the given OpenAPI specification located by a URI. */
    public void compileOas(URI uri) {
      preventRepeatedCodeGen();

      try {
        setSourcePaths(ctx, CompilerSupport.compileOas(getPackage(ctx), uri));
      } catch (OasParseException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Generates and compiles java sources for the given OpenAPI specification.
     *
     * @param specification An OpenAPI document as a string.
     * @see CompilerSupport#compileOas(String, String)
     */
    public void compileOas(String specification) {
      preventRepeatedCodeGen();

      try {
        setSourcePaths(ctx, CompilerSupport.compileOas(getPackage(ctx), specification));
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

      source = source.replaceAll("\\{\\{package}}", getPackage(ctx));

      for (var i = 0; i < kvs.length / 2; i += 2) {
        var key = kvs[i];
        var value = kvs[i + 1];
        source = source.replaceAll("\\{\\{" + key + "}}", value);
      }

      return CompilerSupport.evaluate(getPackage(ctx), returnType, source);
    }

    public String getFileStringForClass(String classname) {
      classname = classname.replace("{{package}}", getPackage(ctx));
      var path = getSourcePaths(ctx).get(classname);
      try {
        return Files.readString(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public Class<?> getClassForName(String template) throws ClassNotFoundException {
      template = template.replace("{{package}}", getPackage(ctx));
      return Class.forName(template);
    }

    private void preventRepeatedCodeGen() {
      if (getSourcePaths(ctx) != null) {
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
    }
  }
}
