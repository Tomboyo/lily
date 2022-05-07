package com.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.Writer;

class SimpleExplodeFactory extends JsonFactory {
  @Override
  public JsonGenerator createGenerator(Writer w) {
    return new SimpleGenerator(w, "=");
  }
}
