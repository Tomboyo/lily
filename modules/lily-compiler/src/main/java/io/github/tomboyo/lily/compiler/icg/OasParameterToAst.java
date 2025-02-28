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
import io.github.tomboyo.lily.compiler.oas.model.*;
import java.util.Optional;
import java.util.stream.Stream;

public class OasParameterToAst {

  public static Optional<ParameterAndAst> evaluateParameter(
      PackageName basePackage, PackageName genRoot, IParameter iParameter) {
    return switch (iParameter) {
      // TODO: handle Ref correctly
      case None none -> Optional.empty();
      case Ref ref -> Optional.empty();
      case Parameter parameter -> Optional.of(evaluateSchema(basePackage, genRoot, parameter));
    };
  }

  private static ParameterAndAst evaluateSchema(
      PackageName basePackage, PackageName genRoot, Parameter parameter) {

    // TODO: handle missing name
    var parameterRefAndAst =
        OasSchemaToAst.evaluateInto(
            basePackage,
            genRoot,
            SimpleName.of(parameter.name().orElseThrow()),
            parameter.schema());

    // TODO: handle missing in
    var location = ParameterLocation.fromString(parameter.in().orElseThrow());

    var encoding =
        parameter
            .style()
            .map(style -> getExplicitEncoding(style, parameter.explode()))
            .orElseGet(() -> getDefaultEncoding(location));

    // TODO: handle missing name
    return new ParameterAndAst(
        new OperationParameter(
            SimpleName.of(parameter.name().orElseThrow()),
            parameter.name().orElseThrow(),
            location,
            encoding,
            parameterRefAndAst.left()),
        parameterRefAndAst.right());
  }

  private static ParameterEncoding getExplicitEncoding(String style, IExplode explode) {
    return switch (OasStyle.forString(style)) {
      case SIMPLE ->
          switch (explode) {
            case Explode(boolean value) when value -> simpleExplode();
            case Explode x -> simple();
            case None x -> simple();
          };
      case FORM ->
          switch (explode) {
            case Explode(boolean value) when value -> formExplode();
            case Explode x -> form();
            case None x -> form();
          };
      case MATRIX, DEEP_OBJECT, LABEL, PIPE_DELIMITED, SPACE_DELIMITED, UNKNOWN -> unsupported();
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
