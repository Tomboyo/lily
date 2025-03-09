/**
 * Data types representing elements of an OpenAPI specification.
 *
 * <p>The {@link io.github.tomboyo.lily.compiler.oas.OasReader} configures the ObjectMapper used to
 * deserialize the oas model. No value ever deserializes to null; "empty" values are represented by
 * empty collections, empty optionals, or a None type in a sealed interface.
 */
package io.github.tomboyo.lily.compiler.oas.model;
