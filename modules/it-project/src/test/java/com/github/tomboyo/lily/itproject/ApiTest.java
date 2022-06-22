package com.github.tomboyo.lily.itproject;

import com.example.Api;
import org.junit.jupiter.api.Test;

/** Tests for the generated API. If this code compiles, the generator worked. */
public class ApiTest {
  @Test
  public void operationsAreCategorizedByTag() {
    // Operations are categorized by their tags, potentially under multiple tags.
    new Api().catsOperations().getPetExistsOperation().requestBuilder();
    new Api().dogsOperations().getPetExistsOperation().requestBuilder();

    // Untagged operations are categorized under "other"
    new Api().otherOperations().getStatusOperation().requestBuilder();
  }
}
