package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.AstSupport.fqnPlaceholder;
import static io.github.tomboyo.lily.compiler.ast.ParameterLocation.PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.ParameterLocation;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class OasParameterToAstTest {
  @Test
  void parameterNamedForOasParameterName() {
    var actual =
        OasParameterToAst.evaluateParameter(
            PackageName.of("p"),
            new Parameter().name("name").in("path").schema(new StringSchema()));

    assertThat(actual.parameter().name(), is(SimpleName.of("name")));
  }

  @Test
  void parameterLocationIsAsGiven() {
    try (var astParameterLocation = mockStatic(ParameterLocation.class);
        var oasSchemaToAst = mockStatic(OasSchemaToAst.class)) {
      oasSchemaToAst
          .when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
          .thenReturn(new Pair<>(fqnPlaceholder(), Stream.of()));
      astParameterLocation.when(() -> ParameterLocation.fromString(any())).thenReturn(PATH);

      var actual =
          OasParameterToAst.evaluateParameter(
              PackageName.of("p"), new Parameter().name("name").in("location"));

      assertThat(
          "The parameter location is as given by AstParameterLocation",
          actual.parameter().location(),
          is(PATH));
    }
  }

  @Test
  void parameterAstIsAsGiven() {
    try (var mock = mockStatic(OasSchemaToAst.class)) {
      var ref = fqnPlaceholder();
      var ast = AstClass.of(fqnPlaceholder(), List.of());
      mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
          .thenReturn(new Pair<>(ref, Stream.of(ast)));

      var actual =
          OasParameterToAst.evaluateParameter(
              PackageName.of("p"),
              new Parameter().name("name").in("path").schema(new ObjectSchema()));

      assertThat(
          "The type name is returned as given by OasSchemaToAst",
          actual.parameter().typeName(),
          sameInstance(ref));
      assertThat(
          "The ast stream is returned as given by OasSchemaToAst",
          actual.ast().collect(Collectors.toSet()),
          hasItem(sameInstance(ast)));
    }
  }
}
