package com.github.tomboyo.lily;


import com.github.tomboyo.lily.compile.LilyCompiler;
import com.github.tomboyo.lily.compile.OasParseException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Paths;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

@Mojo(name = "compile-client", defaultPhase = GENERATE_SOURCES)
public class MyMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}")
  private MavenProject project;

  @Parameter(property = "source", required = true)
  private String source;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources", property = "outputDir", required = true)
  private String outputDirectory;

  @Parameter(property = "basePackage", required = true)
  private String basePackage;

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Compiling OAS to " + outputDirectory);
      LilyCompiler.compile(source, Paths.get(outputDirectory), basePackage);
    } catch (OasParseException | RuntimeException e) {
      throw new MojoExecutionException("Cannot compile OAS document", e);
    }

    project.addCompileSourceRoot(outputDirectory);
  }
}
