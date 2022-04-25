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
import com.github.tomboyo.lily.compiler.ast.AstOperationsClass;
import com.github.tomboyo.lily.compiler.ast.AstOperationsClassAlias;
import com.github.tomboyo.lily.compiler.ast.AstReference;
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
    } else if (ast instanceof AstOperationsClass astOperationsClass) {
      return self.renderAstOperationClass(astOperationsClass);
    } else if (ast instanceof AstOperationsClassAlias astOperationsClassAlias) {
      return self.renderAstOperationClassAlias(astOperationsClassAlias);
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
            ) {
              {{{simplePathEncoding}}}
              {{{simpleExplodePathEncoding}}}
            }
            """,
            "renderClass",
            Map.of(
                "packageName",
                ast.packageName(),
                "recordName",
                capitalCamelCase(ast.name()),
                "fields",
                ast.fields().stream().map(this::recordField).collect(Collectors.joining(",\n")),
                "simplePathEncoding",
                simplePathEncoding(ast),
                "simpleExplodePathEncoding",
                simpleExplodePathEncoding(ast)));

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

  // explode? empty string array            object
  // n        n/a   blue   blue,black,brown R,100,G,200,B,150
  private String simplePathEncoding(AstClass ast) {
    return writeString(
        """
        public String simplePathEncoding() {
          return
              {{{expression}}};
        }
        """,
        "simplePathEncoding",
        Map.of(
            "expression",
            ast.fields().stream()
                .map(this::simplePathEncodingPart)
                .collect(Collectors.joining(" + \",\" + \n"))));
  }

  private String simplePathEncodingPart(AstField field) {
    return writeString(
        """
        "{{name}}" + ","  + {{name}}""", "simplePathEncodingPart", field);
  }

  // explode? empty string array            object
  // y        n/a   blue   blue,black,brown R=100,G=200,B=150
  private String simpleExplodePathEncoding(AstClass ast) {
    return writeString(
        """
        public String simpleExplodePathEncoding() {
          return
              {{{expression}}};
        }
        """,
        "simpleExplodePathEncoding",
        Map.of(
            "expression",
            ast.fields().stream()
                .map(this::simpleExplodePathEncodingPart)
                .collect(Collectors.joining(" + \",\" + \n"))));
  }

  private String simpleExplodePathEncodingPart(AstField field) {
    return writeString(
        """
        "{{name}}" + "="  + {{name}}""", "simpleExplodePathEncodingPart", field);
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
              private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
              private final java.net.http.HttpClient httpClient;

              public {{className}}(
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                  java.net.http.HttpClient httpClient
              ) {
                this.objectMapper = objectMapper;
                this.httpClient = httpClient;
              }

              {{#operations}}
              {{! Note: Tag types are never parameterized }}
              public {{fqReturnType}} {{operationName}}() {
                return new {{fqReturnType}}(this.objectMapper, this.httpClient);
              }

              {{/operations}}
            }
            """,
            "renderAstApi",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name()),
                "operations",
                    ast.astOperationsAliases().stream()
                        .map(
                            alias ->
                                Map.of(
                                    "fqReturnType", fqn(alias),
                                    "operationName", lowerCamelCase(alias.name())))
                        .collect(toList())));

    return new Source(filePath(ast), content);
  }

  private Source renderAstOperationClass(AstOperationsClass ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            public class {{className}} {
              private {{className}}() {}

            {{! Operations are complex, so render them separately. }}
            {{{operations}}}
            }
            """,
            "renderAstOperationClass",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name()),
                "operations",
                    ast.operations().stream()
                        .map(this::renderOperation)
                        .collect(Collectors.joining("\n"))));

    return new Source(filePath(ast), content);
  }

  private String renderOperation(AstOperation ast) {
    return writeString(
        """
              public static com.github.tomboyo.lily.http.HttpHelper<
                  {{{fqpResponseName}}}
              > {{operationName}}(
                  com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                  java.net.http.HttpClient httpClient
              ) {
                return new com.github.tomboyo.lily.http.HttpHelper(
                    httpClient,
                    new com.github.tomboyo.lily.http.JacksonBodyHandler(
                        objectMapper,
                        new com.fasterxml.jackson.core.type.TypeReference<{{{fqpResponseName}}}>(){}),
                    java.net.http.HttpRequest.newBuilder());
              }
            """,
        "renderOperation",
        Map.of(
            // TODO: real response type
            "fqpResponseName", "java.lang.String", "operationName", lowerCamelCase(ast.id())));
  }

  private Source renderAstOperationClassAlias(AstOperationsClassAlias ast) {
    var content =
        writeString(
            """
          package {{packageName}};
          public class {{className}} {
            private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
            private final java.net.http.HttpClient httpClient;

            public {{className}}(
                com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                java.net.http.HttpClient httpClient
            ) {
              this.objectMapper = objectMapper;
              this.httpClient = httpClient;
            }

            {{#operations}}
            public com.github.tomboyo.lily.http.HttpHelper<{{{fqpResponseName}}}> {{operationName}}() {
              return {{operationsClassName}}.{{operationName}}(this.objectMapper, this.httpClient);
            }
            {{/operations}}
          }
          """,
            "renderAstOperationClassAlias",
            Map.of(
                "packageName", ast.packageName(),
                "className", capitalCamelCase(ast.name()),
                "operations",
                    ast.aliasedOperations().stream()
                        .map(
                            operation ->
                                Map.of(
                                    // TODO: use real response type
                                    "fqpResponseName", "java.lang.String",
                                    "operationsClassName", fqn(ast.operationsSingleton()),
                                    "operationName", lowerCamelCase(operation.id())))
                        .collect(toList())));

    return new Source(filePath(ast), content);
  }
}
