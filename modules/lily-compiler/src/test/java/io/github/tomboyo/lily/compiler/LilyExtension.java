package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSourcesInPackage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Provides per-class lifecycle code generation support. */
public class LilyExtension implements AfterAllCallback, ParameterResolver, Extension {

  private final Map<Class<?>, LilyTestSupport> instances = new HashMap<>();

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    instances.values().forEach(LilyTestSupport::deleteGeneratedSources);
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
    return instances.computeIfAbsent(
        parameterContext.getDeclaringExecutable().getDeclaringClass(),
        key -> new LilyTestSupport());
  }

  public static class LilyTestSupport {
    private String packageName;

    private LilyTestSupport() {}

    public void compileOas(String specification) {
      try {
        packageName = CompilerSupport.compileOas(specification);
      } catch (OasParseException e) {
        throw new RuntimeException(e);
      }
    }

    public String packageName() {
      return packageName;
    }

    public Object evaluate(String source, String... kvs) {
      return evaluate(source, Object.class, kvs);
    }

    public <T> T evaluate(String source, Class<T> returnType, String... kvs) {
      if (kvs.length % 2 != 0) {
        throw new IllegalArgumentException("Must be an even number of key-value pairs");
      }

      if (packageName != null) {
        source = source.replaceAll("\\{\\{package}}", packageName);
      }

      for (var i = 0; i < kvs.length / 2; i += 2) {
        var key = kvs[i];
        var value = kvs[i + 1];
        source = source.replaceAll("\\{\\{" + key + "}}", value);
      }

      return CompilerSupport.evaluate(source, returnType);
    }

    private void deleteGeneratedSources() {
      if (packageName == null) {
        return;
      }
      try {
        deleteGeneratedSourcesInPackage(packageName);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
