package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstParameter;
import io.github.tomboyo.lily.compiler.ast.AstParameterLocation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.stream.Stream;

public class OasParameterToAst {

  public static ParameterAndAst evaluateParameter(String packageName, Parameter parameter) {
    var parameterRefAndAst =
        OasSchemaToAst.evaluate(
            packageName, capitalCamelCase(parameter.getName()), parameter.getSchema());

    return new ParameterAndAst(
        new AstParameter(
            parameter.getName(),
            AstParameterLocation.fromString(parameter.getIn()),
            parameterRefAndAst.left()),
        parameterRefAndAst.right());
  }

  public static record ParameterAndAst(AstParameter parameter, Stream<Ast> ast) {}
}
