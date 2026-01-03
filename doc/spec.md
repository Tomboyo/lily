TODO: rename Templates to Directory

# The Spec

This is a living document describing goals and features both implemented and
aspirational.

## Terms Defined
These are terms we will strive to use consistently throughout the software and
this spec.

**Api (class)**: The `Api` generated class is a collection of static functions
operating at the highest level of abstraction. Used to e.g. send requests.

**Directory (class)**: The `Directory` class is a directory of `Template`s
generated one each per specification operation. It defines a static factory
function to get an instance of each `Template`.

**Template**: A template is used to configure a request for a particular
operation, and may be re-used to "print out" multiple identical or similar
requests. All templates are aggregated by the `Templates` class, which provides
a convenient way to create any given template.

# API
This section describes how a developer will interact with generated sources to
achieve their goals.

## Templates
Each operation described by an OpenAPI specification is compiled into a _template_.

Templates are APIs for creating HTTP requests conforming to an OpenAPI operation
specification. Templates are logic-less containers of data that hold on to
parameter bindings (the `id` in a path like `/foo/{id}/bar`), or 
fully-formed request paths, queries, cookies, and bodies that need no 
further processing.

```java
// A blank template with no bound parameters.
var template = Directory.createPet(); // => CreatePetTemplate

// Bind parameters:
template = template
    .withPathParameters(p -> p.withBaz("baz"))
    .withQueryParameters(p -> p.withFoo("foo"))
    .withCookieParameters(p -> p.withBar("bar"))
    .withHeaderParameters(p -> p.withBiff("biff"))
    .withBody(b -> b.withBang("bang")); // => CreatePetTemplate

// Set overrides, which are fully-realized bespoke values. When templates 
// are used to make http requests, overrides are used in preference to 
// parameter bindings. These are used when something is wrong with the 
// generated code or the underlying specification.
template = template
    .withpathOverride("/my/custom/path")
    .withQueryOverride("?my=custom,query,fragment")
    .withCookieOverride(Map.of(
        "Cookie", List.of("Version=\"1\"", "cats=\"dogs,skunks,mice\"")))
    .withHeaderOverride(Map.of(
        "x-header-name", "x-header-value",
        "biff-header", "biff"))
    .withBodyOverride(myJsonPayload.getBytes()); // Any byte array or byte buffer
```

Templates are immutable so that they can be easily shared.

After binding parameters and/or overrides, the template may be used to get 
the path component, query fragment, headers, cookies, and body of the 
templated request. Note that if an override is configured, it takes 
precedence over any relevant bound parameters.

```java
// The path component of a request with all parameters interpolated, like
// `"/foo/myId/bar"`.
template.getPath();

// The query component of a request with all parameters interpolated, like
// `"?foo=bar&baz=bang"`.
template.getQuery();

// The Map<String, List<String>> of cookies, e.g.
// {"Cookie": ["$Version=\"1\"", "bar=\"bar\"; $Path=\"/\""]}
// (consistent with the java.net.http API)
template.getCookies();

// The bytes of the request body.
template.getBodyByteArray();
```

### Optional Validation

An optional `validate` method may be invoked to check if all required 
parameters have been set, which is intended to be used during development 
and testing to help developers explore an API and check their assumptions:

```java
template.validate(); // => ValidateResult := Valid | Invalid(reason)
```

This returns either a `Valid` record or an `Invalid` record with explanatory 
text, which form the `ValidateResult` sealed interface. This is _optional_ 
since enforced validation can become a burden when specifications contain 
minor errors, and ultimately the service is the source of truth on what is 
and is not valid.

### TODO: Cookies

TODO: The java.net.http cookie management API renders `$` prefixes for cookie
attributes like Path and Version, which is an obsolete practice. Whether we 
imitate this depends on how much it impacts java.net.http interop and the 
user experience.

TODO: Does the OpenAPI specification define the Path, Max-Age, and other
attributes of cookies? If so, will the Template API set those attributes
automatically so that the user needs to only bind one value instead of several?

See https://datatracker.ietf.org/doc/html/rfc6265#section-4.2, which 
specifies how cookies are formatted. The OpenAPI specification lets users 
set a format for cookies, which is probably constrained to the contents of a 
single key-value pair. We will need to confirm.

## Sending Requests

Templates are converted into `HttpRequest`s and dispatched in either of two
ways. The first approach uses the `Api` class, a high-level interface for making
requests:
```java
var response = Api.sendSync(
        HttpClient.newHttpClient(),
        "https://example.com",
        Directory.getPet().withPathParameters(p -> p.withId("foo")));
// => a GetPetResponse
```
If either the specification or Lily is flawed, this approach might not work.
Users can use an equivalent, lower-level API as necessary:
```java
var requestBuilder = HttpRequests.builderFor(
        "https://example.com",
        Directory.getPet().withPathParameters(p -> p.withId("foo")));
// => a java.net.http.HttpRequest.Builder

var httpResponse = HttpClient.newHttpClient()
    .send(request.build(), BodyHandlers.ofByteArray());
// => a java.net.http.HttpResponse

var response = Responses.forCreatePet(response);
// => a CreatePetResponse (which is AutoCloseable)
```
This approach lets users work with the client, request, and response objects
using the native API. Any of the Lily APIs used in the above example could be
replaced with custom code, allowing users to "opt-in" to Lily support and
hand-write whatever else is needed.

## Receiving Responses

The set of response codes and schema documented by an operation are generated
into a sealed interface (e.g. `GetPetResponse permits GetPet200, GetPet404`).

TODO: incomplete. Provide examples.

### Deserialization

The `Api` eagerly deserializes response bytes into their corresponding 
response objects, which is usually what the developer wants. If a 
specification is flawed, though, this can lead to deserialization errors. To 
work around those, developers can use a lower-level API:

```java
var requestBuilder = HttpRequests.builderFor(
    URI.create("https://example.com"),
    Template.getPet().withPathParameters(p -> p.withId("foo")));
// => a java.net.http.HttpRequest.Builder

var httpResponse = HttpClient.newHttpClient()
    .send(request.build(), BodyHandlers.ofByteArray());
// => a java.net.http.HttpResponse

// Write your own status-code checks and body deserialization here, possibly 
// using your own models that work better than the generated models.
```

The deserialization library is an implementation detail. Lily will wrap all 
exceptions from underlying dependencies, but if any are leaked, developers 
should avoid coupling to them (e.g. catch `Exception` rather than 
`VendorSpecificException` when possible.)

## TODO: Models

All schema are generated into jackson-annotated classes that support
serializaiton and deserialization alike. Models are either records or implement
record-like definitions of equals and hashCode.

Because schema may consist of a large number of properties, models all come with
a _builder API_ for creating new instances.

# Requirements
This sections covers "requirements," design decisions that don't fit neatly into
other sections but need to be documented for future reference.

## Backwards-compatibility
When the user upgrades from version A of a specification to version B, assuming
B is backwards compatible with A, the generated code should likewise be
backwards compatible. In other words, the user code written with the client
generated from A should still compile with the client generated from B.

### Anonymous Models

The Template API exposes a set of "anonymized withers" that let the user build a
request without coupling to the names of model classes. Consider the following:

```java
var template = Template.createAccount()
        .withBody(body -> body
                .withAddress(address -> address
                        .withZipCode("1234")
                        .withStreetAddress("123 My Rd.")));
```

This client code does not import a class for the account address and does not
need to reference it by name at all, making it decoupled from the name chosen by
the code generator. Why does this matter? Imagine that version 1 of the
specification defines the account address with an _in-line_ schema, like so:

```yaml
components:
  schemas:
    Account:
      type: object
      properties:
        address: # <---
          type: object
          properties:
            zipCode:
              type: string
            streetAddress:
              type: string
```

Then suppose the address definition is moved to a component in version 2:

```yaml
components:
  schemas:
    Account:
      type: object
      properties:
        address:
          $ref: '#/components/schemas/Address'
    Address: # <---
      type: object
      properties:
        zipCode:
          type: string
        streetAddress:
          type: string
```

The name of the address class generated from version 1 will probably be
different from the name generated from version 2 (e.g.
`com.example.Account$Address` vs. `com.example.Address`), causing compilation
errors in the client code after updating the specification. However, the API
itself has not changed at all, only the way the API is described.

By anonymizing model names in client code, we limit the surface area of 
client code broken by a specification update. We cannot completely prevent 
it, however. Even with anonymized models, user code will need to reference
models by name in order to create re-usable functions:

```java
public static Address exampleAddress(Address x) {
    return x.withZipCode("1234")
            .withStreetAddress("1234 Example St.");
}
```

## Robustness
The generator should be robust in the face of edge cases and unusual 
specifications. It should do as much as it can to help the user, then get out of
the user's way when it is unsatisfactory.

### No Mandatory Validation

The service itself is the source of truth on what is and is not a valid 
request. While client-side validation may be useful for testing and learning,
_mandatory_ validation runs the risk of preventing a user from making a 
well-formed request if either the specification or the generated code is 
inaccurate.

### Layered API
The generated API is structured in layers, where the most abstract layer targets
common use-cases and is built from the less-abstract layer beneath it. The user
will use more-abstract layers whenever they are convenient, and dip into 
less-abstract underlying layers when necessary. The less-abstract layers try 
to provide access to all the same utility as their more-abstract 
counterparts, but give the user more control in exchange for convenience.

> "Simple things should be simple and complex things should be possible."
>
> \- Alan Kay.


Refer to the [Sending Requests](#sending-requests) section for an example where
the `Api` abstraction is used to send requests, but the underlying native 
Http layer is available in conjunction with the `HttpRequests` and 
`Responses` helper libraries. All the same functionality is present in both 
layers, but they differ in control and convenience.

### Generated code does not use import statements
Generated code should always use fully-qualified class names rather than import
statements to avoid import collisions like the following:
```java
import java.util.Consumer;
import com.example.model.Consumer;
```
Import statements are a convenience for humans authors. Generated code does not need them.

### Graceful Failure
Whenever the code generator encounters something it does not know what to do 
with, it should _not_ throw an exception or otherwise halt code generation. 
Instead, it should log a warning to the user and generate what code it can, 
providing the user as much value as possible.

## Not Quite RPC
OpenAPI specifications document http services in whatever shape and size they
happen to be in, meaning anything that is _possible_ may be reflected in a 
specification. Unfortunatley, this means generated code can't quite separate 
itself from the medium of HTTP and present users with a "business-oriented" API
like you might get from a RPC-style specification and code generator. 
Consider the following:

- As a user, I would like to simply bind named parameters for a request and 
  send it off, ignorant to whether those parameters are in the query, header, 
  body, or cookie. Unfortunately, a parameter in the header _can_ have the same 
  name as a parameter in the path, query, cookie, or body in the OpenAPI 
  standard, meaning the location of each parameter is effectively part of the
  parameter's identifier. This bars us from (consistently, safely) 
  generating "business-oriented" code unless we make certain assumptions 
  about the server's API design that the owners of that API have not 
  themselves made (or at least have not communicated making). If they want 
  to commit to those decisions, they could use something _other_ than OpenAPI.

In short, OpenAPI is a powerful tool for documenting services and is far 
better than nothing at all, but is coupled to the HTTP medium of exchange 
and leads to generated source code that is necessarily also coupled to HTTP. 
The code generated by Lily does not try to fight this, since it's futile. 
Instead, we try to remain aware of this coupling problem and express that 
coupling when necessary in the API.