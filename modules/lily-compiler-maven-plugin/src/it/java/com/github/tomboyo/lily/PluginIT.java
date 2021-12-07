package com.github.tomboyo.lily;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * Build a project using this plugin, which compiles and runs tests against the generated sources.
 *
 * <p>Note: A plugin cannot be referenced within the multi-module build it is built in, which is why
 * we need to create a project external to this build process.
 */
public class PluginIT {
  @Test
  public void buildProjectAndRunTests() throws Exception {
    // Build and execute the it-project test project.
    var result =
        new DefaultInvoker()
            .setMavenHome(new File(System.getenv("MAVEN_HOME")))
            .execute(
                new DefaultInvocationRequest()
                    .setPomFile(new File("it-project/pom.xml"))
                    .setGoals(List.of("clean", "verify")));

    if (result.getExitCode() != 0) {
      throw new Exception("IT-Project build failed. Check logs for root cause.");
    }
  }
}
