package com.github.tomboyo.lily;

import com.github.tomboyo.lily.cli.Compile;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new Compile()).execute(args);
    System.exit(exitCode);
  }
}
