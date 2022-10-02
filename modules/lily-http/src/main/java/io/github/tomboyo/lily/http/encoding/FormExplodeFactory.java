package io.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.Writer;

class FormExplodeFactory extends JsonFactory {

  private final String leadingCharacter;

  public FormExplodeFactory(String leadingCharacter) {
    this.leadingCharacter = leadingCharacter;
  }

  @Override
  public JsonGenerator createGenerator(Writer w) {
    return new FormExplodeGenerator(leadingCharacter, w);
  }
}
