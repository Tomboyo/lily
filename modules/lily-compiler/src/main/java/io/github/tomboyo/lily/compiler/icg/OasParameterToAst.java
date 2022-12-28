package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.form;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.formExplode;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.simple;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.simpleExplode;
import static io.github.tomboyo.lily.compiler.ast.ParameterEncoding.unsupported;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.OperationParameter;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.ParameterEncoding;
import io.github.tomboyo.lily.compiler.ast.ParameterLocation;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import java.util.stream.Stream;

public class OasParameterToAst {

  public static ParameterAndAst evaluateParameter(PackageName packageName, Parameter parameter) {
    var parameterRefAndAst =
        OasSchemaToAst.evaluate(
            packageName, SimpleName.of(parameter.getName()), parameter.getSchema());

    var location = ParameterLocation.fromString(parameter.getIn());

    var encoding =
        parameter.getStyle() != null
            ? getExplicitEncoding(parameter.getStyle(), parameter.getExplode())
            : getDefaultEncoding(location);

    return new ParameterAndAst(
        new OperationParameter(
            SimpleName.of(parameter.getName()),
            parameter.getName(),
            location,
            encoding,
            parameterRefAndAst.left()),
        parameterRefAndAst.right());
  }

  private static ParameterEncoding getExplicitEncoding(StyleEnum style, boolean explode) {
    return switch (style) {
      case SIMPLE -> explode ? simpleExplode() : simple();
      case FORM -> explode ? formExplode() : form();
      case MATRIX, DEEPOBJECT, LABEL, PIPEDELIMITED, SPACEDELIMITED -> unsupported();
    };
  }

  private static ParameterEncoding getDefaultEncoding(ParameterLocation location) {
    return switch (location) {
      case PATH, HEADER -> simple();
      case QUERY, COOKIE -> formExplode();
    };
  }

  public static record ParameterAndAst(OperationParameter parameter, Stream<Ast> ast) {}
}
