package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OasApiResponseToAstTest {

  @Nested
  class EvaluateApiResponse {
    @Test
    void generatesAstForJsonResponses() {
      try (var mock = mockStatic(OasSchemaToAst.class)) {
        mock.when(() -> OasSchemaToAst.evaluate(any(), any(), any()))
            .thenAnswer(invocation -> new Pair<>(astBoolean(), Stream.of()));

        OasApiResponseToAst.evaluateApiResponse(
            PackageName.of("com.example"),
            SimpleName.of("GetFoo"),
            "200",
            new ApiResponse()
                .content(
                    new Content()
                        .addMediaType(
                            "application/json", new MediaType().schema(new ObjectSchema()))
                        .addMediaType(
                            "application/xml", new MediaType().schema(new ObjectSchema()))));

        mock.verify(
            () ->
                OasSchemaToAst.evaluate(
                    eq(PackageName.of("com.example")), eq(SimpleName.of("GetFoo200")), any()));
        mock.verifyNoMoreInteractions();
      }
    }
  }
}
