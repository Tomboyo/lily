# The Spec

This is a living document describing goals and features both implemented and
aspirational. The goal of this document is to focus on how Lily should work and
why it should work that way, but not how it works -- implementation details
should be reserved for the code.

# API

This section describes how a developer will interact with generated sources to
achieve their goals.

The API strives to be easy to use, backwards- and forwards-compatible, and
discoverable to a user with an IDE.

## `Lily` - Make Requests And Read Responses

The _Lily_ API **creates HTTP requests from _Operations_ and parses HTTP responses
into response types**. The response types are sealed interfaces with one
permitted member per documented HTTP response code, plus a catch-all for
unexpected responses.

```java
Lily.sendSync(
        HttpClient.newBuilder().build(),
        myHttpBaseUrl, // "https://example.com/base/path"
        Operations.getPet(),
        parameters -> parameters
            .bindPathParameters(p ->
                p.bindId("mock-id"))
            .bindQueryParameters(p ->
                p.bindFoo("foo!")
                 .bindBar("bar!")
                 .bindBaz("baz!")))
// => Result<PetStoreResponse, Lily.SendSyncError>
    .reduce(
        ok -> switch (ok) {
          case GetPet200 x -> Result.ok(x.body());
          case GetPet404 _ignored -> Result.error(new MyError.DoesNotExist());
        },
        err -> Result.error(new MyError.RequestError(err)));
// => Result<Pet, MyError>

public sealed interface MyError {
  record DoesNotExist implements MyError {}
  record RequestError(Lily.SendSyncError cause) implements MyError {}
}
```

## `Operations` - Request And Response Type Pairs

To make requests, the `Lily.sendSync` method requires an _operation_. Operations
**describe all the coordinating parts of a request and couple them together**,
including the path template, query template, types of parameters, and types of
responses.

For each operation defined in the OpenAPI specification for a service, there is
a corresponding `Operations.operationName()` function that returns the operation
instance. This arrangement should **make it easy for users to discover
operations with their IDE's code completion** prompts.

```java
Operations.createPet();
Operations.deletePet();
Operations.searchPets();
// And so on.
```

## `ParameterBindings` - Bind Request Parameters

**_Parameter bindings_ help the user bind parameters to an HTTP request using
the domain language of a particular OpenAPI operation**. Each parameter bindings
instance is composed of `IParameters` objects to hold the path, query, header,
and cookie parameters particular to an operation, as well as an object for the
body.

`IParameters` classes implement "binders" that associate ("bind") values to a
request for each of the parameters declared in the OpenAPI specification. This
API is not part of `IParameters` interface because it must be implemented for
whatever parameters happen to exist per operation; i.e. must be generated.
**Binders should produce immutable copies-with-modifications to encourage
re-use.**

```
var parameters = Operations.getPet().parameters();
parameters 
    .bindPathParameters(p ->
        p.bindId("mock-id"))
    .bindQueryParameters(p ->
        p.bindFoo("foo!")
         .bindBar("bar!")
         .bindBaz("baz!")))
// => an immutable copy with the given bound parameters.
```

After parameters are bound, they may be retrieved through a set of accessors:

```java
parameters.pathParameters();       // => extends IParameters
parameters.queryParameters();      // => extends IParameters
parameters.headerParameters();     // => extends IParameters
parameters.cookieParameters();     // => extends IParameters

// The generated model (DTO) with bound parameters, e.g. CreatePetBody
parameters.body();
```

All `IParameters` may be converted to a `Map<String, Object>` via `asMap()`,
e.g. `parameters.headerParameters().asMap()`, which enables Lily to generically
create HTTP requests using arbitrary parameter binding classes.

- `ParameterBindings` and `IParameters` implementations use anonymous callbacks
  for parameter binding (`bind*Parameters`) per [rule 1.a](#1a-encourage-anonymous-references-to-generated-types).
- All types are immutable so that they can be shared and re-used throughout an
  application.
- `ParameterBindings` and `IParameters` are **logic-less** data containers. The
  knowledge of how to translate parameter bindings to HttpRequests is separate
  from these types. This forces their API to expose all the information
  necessary to create an HTTP Request and ensures a user could therefore write
  their own method for creating HTTP requests from them if desired.

## TODO: Models

All schema (e.g. `#/components/schema`) are generated into jackson-annotated
classes that support serialization and deserialization alike. Models are either
records or implement record-like definitions of equals and hashCode.

Each model has an associated builder per [rule
1.b](#1b-use-builders-to-bind-parameters).

TODO: discuss how models are generated from e.g. composed schema.

## Optional Validation

Users may optionally `validate()` a `ParameterBindings` to check if all required
parameters have been set, which is intended to be used during development and
testing to help developers explore an API and check their assumptions.

* This is _optional_ since enforced validation can become a burden when
specifications contain minor errors, and ultimately the service is the source of
truth on what is and is not valid.

# When Things Go Wrong

Some OpenAPI specifications are incomplete or inaccurate. Owners of OpenAPI
specifications may not be able to quickly address any specification errors
reported by spec consumers, forcing those consumers to either forgo using code
generation or hand-write "filler" code to work around the un-generateable
operations they need. Lily addresses this issue by letting users write
"patch files".

Patch files modify how Lily generates code from an OpenAPI specification. When
Lily reads on OpenAPI specification file, it will add or modify the elements it
reads based on the contents of the patch file. This lets the consumer achieve a
variety of goals, including:
* Add a missing parameter to an operation,
* fix the name of a schema,
* fix the style and/or format of a parameter,
* or add completely missing operations or schemas.

Lily does not modify the OpenAPI specification itself during this process.

* Patch files allow developers to "layer" corrections on top of a source
  document in a maintainable way. Compared to directly editing a source file,
  patch file make it clear what customizations a developer has made, which
  simplifies updates to newer versions of a specificaton over time.
* Patch files let developers fix issues at the "greatest point of leverage."
  Patch files address issues once and avoid littering work-around code
  throughout a project.
* Patch files obviate the need for the generated API to support customizations.
  The generated API stays focused on the "happy path" while patching focuses on
  corrections. Further, users are less likely to need to access underlying
  layers of Lily (e.g. the java.net.http layer), decreasing potential coupling.

# Requirements

## 1. Backwards-Compatibility

When the user upgrades from version A of a specification to version B, assuming
B is backwards compatible with A, the generated code should likewise be
backwards compatible. In other words, the user code written with the client
generated from A should still compile with the client generated from B.

### 1.a Encourage Anonymous References To Generated Types

TODO: response sum type names and response body models are both subject to 
the problem of name changes over time, and there isn't an obvious way to 
avoid coupling to them. Make a note about that in this section.

The names of entities defined in an OpenAPI specification or the names generated
by Lily for those entities, including but not limited to the names of schema and
parameters, may change between versions of an OpenAPI specification for an API.
These changes may occur even if two such versions of a specification are
compatible with one another from the perspective of a human reader, for example
when an _anonymous_ schema (a.k.a. an "in-line" schema) is moved to
`#/components/schemas` and becomes named. As a result, if user code refers to
generated types statically by name, such as in an import statement, their code
can fail to compile after updating their source specification to the latest
version. To avoid that, we should favor an API that insulates users from type
names where possible.

* **Lambda parameters**: Lambda parameters allow the user to interact with
  concrete types without coupling to their class name:
  ```java
  Example.example(p -> p.bindX("x!").bindY("y!"));
  ```
  This approach allows a user to bind parameters with a fluent builder API
  without importing the name of a generated record which possesses those
  parameters. As long as `p` maintains a backwards-compatible API, this approach
  continues to type-check. Meanwhile, this approach is discoverable to most
  IDEs, which can tell the user the type of `p` and enumerate its methods.

### 1.b Use Builders To Bind Parameters

Favor builder APIs to bind all parameters to an operation.

The set of parameters accepted by an operation (including path, query, cookie,
header, and body parameters) may change over time. When a parameter is added to
a specification, client code built from an earlier version of the specification
should still work. This means generated APIs should not use positional
arguments to represent non-required operation parameters, since a change to the
number of such arguments is a breaking change. Builder APIs insulate client
code from the number of parameters in an operation and are therefore favored.

Likewise, do not represent _required_ parameters with positional parameters,
since the user may override what parameters are "required" with a patch file,
and since the order in which those parameters appear should be irrelevant to the
generated API and user code.

## 2. Robustness

The generator should be robust in the face of edge cases and unusual 
specifications. It should do as much as it can to help the user, then get out of
the user's way when it is unsatisfactory.

### 2.a No Mandatory Validation

The service itself is the source of truth on what is and is not a valid request.
While client-side validation is useful for testing and development, it forces
developers to patch out a larger number of otherwise benign specification
errors. Developers with inadequate tests could furthermore encounter production
defects, which is unacceptable if the requests are actually well-formed
according to the server!

### 2.b Generated code does not use import statements
Generated code should always use fully-qualified class names rather than import
statements to avoid import collisions like the following:
```java
import java.util.Consumer;
import com.example.model.Consumer;
```
Import statements are a convenience for human authors. Generated code does not
need them.

### 2.c Graceful Failure
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
