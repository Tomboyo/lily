package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.AstSupport.astReferencePlaceholder;
import static io.github.tomboyo.lily.compiler.ast.AstParameterLocation.PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.AstParameterLocation;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
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
    try (var astParameterLocation = mockStatic(AstParameterLocation.class);
        var oasSchemaToAst = mockStatic(OasSchemaToAst.class)) {
      oasSchemaToAst
          .when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
          .thenReturn(new Pair<>(astReferencePlaceholder(), Stream.of()));
      astParameterLocation.when(() -> AstParameterLocation.fromString(any())).thenReturn(PATH);

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
      var ref = astReferencePlaceholder();
      var ast = astReferencePlaceholder();
      mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
          .thenReturn(new Pair<>(ref, Stream.of(ast)));

      var actual =
          OasParameterToAst.evaluateParameter(
              PackageName.of("p"),
              new Parameter().name("name").in("path").schema(new ObjectSchema()));

      assertThat(
          "The astReference is returned as given by OasSchemaToAst",
          actual.parameter().astReference(),
          sameInstance(ref));
      assertThat(
          "The ast stream is returned as given by OasSchemaToAst",
          actual.ast().collect(Collectors.toSet()),
          hasItem(sameInstance(ast)));
    }
  }
}
