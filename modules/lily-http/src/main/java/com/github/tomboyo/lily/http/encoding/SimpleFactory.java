package com.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.Writer;

class SimpleFactory extends JsonFactory {
  @Override
  public JsonGenerator createGenerator(Writer w) {
    // TODO: feature AUTO_CLOSE_TARGET?
    return new SimpleGenerator(w);
  }
}
