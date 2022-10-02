package io.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.Writer;

class FormExplodeFactory extends JsonFactory {
  @Override
  public JsonGenerator createGenerator(Writer w) {
    return new FormExplodeGenerator("?", w);
  }
}
