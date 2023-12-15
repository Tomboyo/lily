package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.Mustache.writeString;
import static io.github.tomboyo.lily.compiler.cg.support.Interfaces.implementsClause;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.cg.support.Fields;
import java.util.Map;
import java.util.stream.Collectors;

public class AstClassCodeGen {

  public static Source renderClass(AstClass ast) {
    var content =
        writeString(
            """
            package {{packageName}};
            /**
             {{docstring}}

             <p> Generated by Lily
             */
            public record {{TypeName}}(
                {{{recordFields}}}
            ) {{implementsClause}} {

              public static {{TypeName}}.Builder newBuilder() {
                return new {{TypeName}}.Builder();
              }

              {{{propertyGetters}}}

              public static class Builder {
                {{{builderFields}}}
                {{{propertySetters}}}
                {{{buildUnvalidated}}}
              }
            }
            """,
            "renderClass",
            Map.of(
                "packageName",
                ast.name().packageName(),
                "TypeName",
                ast.name().typeName().upperCamelCase(),
                "recordFields",
                Fields.recordFields(ast.fields()),
                "propertyGetters",
                ast.fields().stream()
                    .map(AstClassCodeGen::propertyGetter)
                    .collect(Collectors.joining("\n")),
                "docstring",
                ast.docstring(),
                "implementsClause",
                implementsClause(ast),
                "builderFields",
                ast.fields().stream()
                    .map(AstClassCodeGen::builderField)
                    .collect(Collectors.joining("\n")),
                "propertySetters",
                ast.fields().stream()
                    .map(field -> propertySetter(ast, field))
                    .collect(Collectors.joining("\n")),
                "buildUnvalidated",
                buildUnvalidated(ast)));

    return new Source(ast.name(), content);
  }

  private static String propertyGetter(Field field) {
    var fqpt = field.astReference().toFqpString();
    var returned = Fields.fieldName(field);
    if (!field.isMandatory()) {
      fqpt = "java.util.Optional<" + fqpt + ">";
      returned = "java.util.Optional.ofNullable(" + returned + ")";
    }
    return writeString(
        """
            public {{{fqpt}}} {{getterName}}() {
              return {{returned}};
            }
            """,
        "AstClassCodeGen.propertyGetter",
        Map.of(
            "fqpt", fqpt,
            "getterName", Fields.getterNameForField(field),
            "returned", returned));
  }

  private static String builderField(Field field) {
    return "private " + field.astReference().toFqpString() + " " + Fields.fieldName(field) + ";";
  }

  private static String propertySetter(Ast ast, Field field) {
    return writeString(
        """
                    public {{{builderName}}} set{{Name}}({{{type}}} {{name}}) {
                      this.{{name}} = {{name}};
                      return this;
                    }
                    """,
        "AstClassCodeGen.propertySetter",
        Map.of(
            "builderName", ast.name().typeName().upperCamelCase() + ".Builder",
            "Name", field.name().upperCamelCase(),
            "name", Fields.fieldName(field),
            "type", field.astReference().toFqpString()));
  }

  public static String buildUnvalidated(AstClass ast) {
    return writeString(
        """
            public {{{Name}}} buildUnvalidated() {
              return new {{{Name}}}(
                  {{{fields}}}
              );
            }
            """,
        "AstClassCodeGen.buildUnvalidated",
        Map.of(
            "Name", ast.name().toFqpString(),
            "fields",
                ast.fields().stream().map(Fields::fieldName).collect(Collectors.joining(",\n"))));
  }
}
