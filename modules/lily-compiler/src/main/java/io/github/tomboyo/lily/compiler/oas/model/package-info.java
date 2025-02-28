/**
 * Data types representing elements of an OpenAPI specification.
 *
 * <p>The {@link io.github.tomboyo.lily.compiler.oas.OasReader} configures the ObjectMapper used to
 * deserialize the oas model. No value ever deserializes to null; "empty" values are represented by
 * empty collections, empty optionals, or a None type in a sealed interface.
 *
 * <p>All sealed interfaces in this package have a None type. If such an interface is an argument to
 * a JsonCreator, however, the argument may be null. Whether the argument is null depends on if the
 * JsonCreator for the interface was actually invoked or not. If the field corresponding to the
 * argument was completely missing, it will default to null. If the key was present but all fields
 * were missing/empty within the object, it will be None.
 *
 * <p>Also note that arguments to a JsonCreator can be null even if the default empty value should
 * be an empty collection. This is because those creators technically override the default behavior,
 * I think.
 */
package io.github.tomboyo.lily.compiler.oas.model;
