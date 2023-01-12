package io.github.tomboyo.lily.compiler.cg;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import java.io.StringReader;
import java.io.StringWriter;

public class Mustache {

  private static final MustacheFactory FACTORY = new DefaultMustacheFactory();

  public static String writeString(String template, String name, Object scopes) {
    var mustache = FACTORY.compile(new StringReader(template), name);
    var stringWriter = new StringWriter();
    mustache.execute(stringWriter, scopes);
    return stringWriter.toString();
  }
}
