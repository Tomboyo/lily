package io.github.tomboyo.lily;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import io.github.tomboyo.lily.compiler.LilyCompiler;
import io.github.tomboyo.lily.compiler.OasParseException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "compile-client", defaultPhase = GENERATE_SOURCES)
public class MyMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}")
  private MavenProject project;

  @Parameter(property = "url", required = true)
  private String url;

  @Parameter(
      defaultValue = "${project.build.directory}/generated-sources",
      property = "outputDir",
      required = true)
  private String outputDirectory;

  @Parameter(property = "basePackage", required = true)
  private String basePackage;

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Compiling OAS to " + outputDirectory);
      LilyCompiler.compile(URI.create(url).toURL(), Paths.get(outputDirectory), basePackage);
    } catch (OasParseException | RuntimeException | MalformedURLException e) {
      throw new MojoExecutionException("Cannot compile OAS document", e);
    }

    project.addCompileSourceRoot(outputDirectory);
  }
}
