package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.ast.AstEncoding.form;
import static io.github.tomboyo.lily.compiler.ast.AstEncoding.formExplode;
import static io.github.tomboyo.lily.compiler.ast.AstEncoding.simple;
import static io.github.tomboyo.lily.compiler.ast.AstEncoding.simpleExplode;
import static io.github.tomboyo.lily.compiler.ast.AstEncoding.unsupported;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstEncoding;
import io.github.tomboyo.lily.compiler.ast.AstParameter;
import io.github.tomboyo.lily.compiler.ast.AstParameterLocation;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import java.util.stream.Stream;

public class OasParameterToAst {

  public static ParameterAndAst evaluateParameter(PackageName packageName, Parameter parameter) {
    var parameterRefAndAst =
        OasSchemaToAst.evaluate(
            packageName, SimpleName.of(parameter.getName()), parameter.getSchema());

    var location = AstParameterLocation.fromString(parameter.getIn());

    var encoding =
        parameter.getStyle() != null
            ? getExplicitEncoding(parameter.getStyle(), parameter.getExplode())
            : getDefaultEncoding(location);

    return new ParameterAndAst(
        new AstParameter(
            SimpleName.of(parameter.getName()), location, encoding, parameterRefAndAst.left()),
        parameterRefAndAst.right());
  }

  private static AstEncoding getExplicitEncoding(StyleEnum style, boolean explode) {
    return switch (style) {
      case SIMPLE -> explode ? simpleExplode() : simple();
      case FORM -> explode ? formExplode() : form();
      case MATRIX, DEEPOBJECT, LABEL, PIPEDELIMITED, SPACEDELIMITED -> unsupported();
    };
  }

  private static AstEncoding getDefaultEncoding(AstParameterLocation location) {
    return switch (location) {
      case PATH, HEADER -> simple();
      case QUERY, COOKIE -> formExplode();
    };
  }

  public static record ParameterAndAst(AstParameter parameter, Stream<Ast> ast) {}
}
