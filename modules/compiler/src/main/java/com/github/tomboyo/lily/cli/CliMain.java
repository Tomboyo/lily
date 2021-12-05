package com.github.tomboyo.lily.cli;

import picocli.CommandLine;

public class CliMain {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new CompileCommand()).execute(args);
    System.exit(exitCode);
  }
}
