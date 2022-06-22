package com.github.tomboyo.lily.compiler.cg;

import static com.github.tomboyo.lily.compiler.cg.Fqns.filePath;
import static com.github.tomboyo.lily.compiler.cg.Fqns.fqn;
import static com.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;
import static com.github.tomboyo.lily.compiler.icg.Support.lowerCamelCase;
import static java.util.stream.Collectors.toList;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.github.tomboyo.lily.compiler.ast.Ast;
import com.github.tomboyo.lily.compiler.ast.AstApi;
import com.github.tomboyo.lily.compiler.ast.AstClass;
import com.github.tomboyo.lily.compiler.ast.AstClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstField;
import com.github.tomboyo.lily.compiler.ast.AstOperation;
import com.github.tomboyo.lily.compiler.ast.AstReference;
import com.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

public class AstToJava {

  private final MustacheFactory mustacheFactory;

  private AstToJava(MustacheFactory mustacheFactory) {
    this.mustacheFactory = mustacheFactory;
  }

  private <T> String writeString(String template, String name, T scopes) {
    var mustache = mustacheFactory.compile(new StringReader(template), name);
    var stringWriter = new StringWriter();
    mustache.execute(stringWriter, scopes);
    return stringWriter.toString();
  }

  public static Source renderAst(Ast ast) {
    var self = new AstToJava(new DefaultMustacheFactory());

    if (ast instanceof AstClass astClass) {
      return self.renderClass(astClass);
    } else if (ast instanceof AstClassAlias astClassAlias) {
      return self.renderAstClassAlias(astClassAlias);
    } else if (ast instanceof AstApi astApi) {
      return self.renderAstAPi(astApi);
    } else if (ast instanceof AstTaggedOperations astTaggedOperations) {
      return self.renderAstTaggedOperations(astTaggedOperations);
    } else if (ast instanceof AstOperation astOperation) {
      return self.renderAstOperation(astOperation);
    } else {
      throw new IllegalArgumentException("Unsupported AST: " + ast);
    }
  }

  private Source renderClass(AstClass ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fields}}}
            ) {}
            """,
            "renderClass",
            Map.of(
                "packageName",
                ast.packageName(),
                "recordName",
                capitalCamelCase(ast.name()),
                "fields",
                ast.fields().stream().map(this::recordField).collect(Collectors.joining(",\n"))));

    return new Source(filePath(ast), content);
  }

  private String recordField(AstField field) {
    return writeString(
        "{{{fqpt}}} {{name}}",
        "recordField",
        Map.of(
            "fqpt", fullyQualifiedParameterizedType(field.astReference()),
            "name", lowerCamelCase(field.name())));
  }

  private static String fullyQualifiedParameterizedType(AstReference ast) {
    if (ast.typeParameters().isEmpty()) {
      return fqn(ast);
    } else {
      var typeParameters =
          "<%s>"
              .formatted(
                  ast.typeParameters().stream()
                      .map(AstToJava::fullyQualifiedParameterizedType)
                      .collect(Collectors.joining(",")));
      return fqn(ast) + typeParameters;
    }
  }

  private Source renderAstClassAlias(AstClassAlias ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public record {{recordName}}(
                {{{fqpValueName}}} value
            ) {
              @com.fasterxml.jackson.annotation.JsonCreator
              public static {{{recordName}}} creator({{{fqpValueName}}} value) { return new {{recordName}}(value); }

              @com.fasterxml.jackson.annotation.JsonValue
              public {{{fqpValueName}}} value() { return value; }
            }
            """,
            "renderAstClassAlias",
            Map.of(
                "packageName", ast.packageName(),
                "recordName", capitalCamelCase(ast.name()),
                "fqpValueName", fullyQualifiedParameterizedType(ast.aliasedType())));
    return new Source(filePath(ast), content);
  }

  private Source renderAstAPi(AstApi ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {
              {{#tags}}
              {{! Note: Tag types are never parameterized }}
              public {{fqReturnType}} {{methodName}}() {
                return new {{fqReturnType}}();
              }

              {{/tags}}
            }
            """,
            "renderAstApi",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name()),
                "tags",
                    ast.taggedOperations().stream()
                        .map(
                            tag ->
                                Map.of(
                                    "fqReturnType", fqn(tag),
                                    "methodName", lowerCamelCase(tag.name())))
                        .collect(toList())));

    return new Source(filePath(ast), content);
  }

  private Source renderAstTaggedOperations(AstTaggedOperations ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {

              {{#operations}}
              {{! Note: Operation types are never parameterized }}
              public {{fqReturnType}} {{methodName}}() {
                return new {{fqReturnType}}();
              }

              {{/operations}}
            }
            """,
            "renderAstTaggedOperations",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name()),
                "operations",
                    ast.operations().stream()
                        .map(
                            operation ->
                                Map.of(
                                    "fqReturnType", fqn(operation),
                                    "methodName", operation.name()))
                        .collect(toList())));

    return new Source(filePath(ast), content);
  }

  private Source renderAstOperation(AstOperation ast) {
    var content =
        writeString(
            """
        package {{packageName}};
        public class {{className}} {
          public java.net.http.HttpRequest.Builder requestBuilder() {
            return java.net.http.HttpRequest.newBuilder();
          }
        }
        """,
            "renderAstOperation",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name())));

    return new Source(filePath(ast), content);
  }
}
