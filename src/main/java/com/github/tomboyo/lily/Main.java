package com.github.tomboyo.lily;

import com.github.tomboyo.lily.ast.OasSchemaToAst;
import io.swagger.parser.OpenAPIParser;

public class Main {
  public static void main(String[] args) {
    var specPath = "petstore.yaml";
    var parseResult = new OpenAPIParser().readLocation(specPath, null, null);

    var errors = parseResult.getMessages();
    errors.forEach(System.err::println);

    var openApi = parseResult.getOpenAPI();
    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      System.err.println("OAS specification version is not 3.x: version=" + version);
    } else {
      System.out.println("OAS version: version=" + version);
    }

    openApi.getComponents().getSchemas().entrySet().stream()
        .flatMap(entry -> OasSchemaToAst.generateAst("mypackage", entry.getKey(), entry.getValue()))
        .forEach(System.out::println);
  }
}
