# The Spec

This is a living document describing goals and features both implemented and
aspirational.

## Terms Defined
These are terms we will strive to use consistently throughout the software and
this spec.

**Api (class)**: The `Api` generated class is a collection of static functions
operating at the highest level of abstraction. Used to send requests with
minimal ceremony.

**Directory (class)**: The `Directory` class is a directory of `Template`s
generated one each per specification operation. It defines a static factory
function to get an instance of each `Template`.

**Template**: A template is used to configure a request for a particular
operation, and may be re-used to "print out" multiple identical or similar
requests. All templates are aggregated by the `Directory` class, which provides
a convenient way to create any given template.

**HttpRequests (class)**: Converts templates into `java.net.http.HttpRequests`.

**HttpResponses (class)**: Converts `java.net.http.HttpResonse` objects to
generated response objects (e.g. `GetPetResponse`, `GetPet200`, `GetPet404`).

# API

This section describes how a developer will interact with generated sources to
achieve their goals.

Lily is composed of two layers:

* The **domain layer** is the most-abstract layer. It works in terms of the
  domain described by an OpenAPI specification.
* The **Http layer** is the lest-abstract layer. It works in terms of HTTP via
  the `java.net.http` package and the Jackson serialization library.

If an OpenAPI specification is accurate and complete, and if Lily is free of
bugs, users will use the domain layer exclusively. Otherwise, they can drop down
into the Http layer in order to work around flaws in Lily or their OpenAPI
specification.

## Domain Layer API

The domain layer consists of:

- `Api`
- `Operation` and `Operations`
- `RequestTemplate` and `IParameters`

### `Api` - Make Requests And Read Responses

The `Api` class provides the most convenient method for sending requests and
parsing responses. `Api` creates HTTP requests from `RequestTemplate`s and
parses HTTP responses into response types. The response types are sealed
interfaces with one permitted member per documented HTTP response code, plus a
catch-all for unexpected responses.

```java
Api.sendSync(
  HttpClient.newHttpClient(),
  "https://example.com/base/url",
  Operations.getPet(),
  template -> template
    .withPathParameters(p -> p.withId("1234")));
// => GetPetResponse (e.g. GetPet200, GetPet404, or GetPetError)
```

### `Operation` and `Operations` - Request And Response Type Pairs

To make requests, the `Api.sendSync` method requires an instance of `Operation`.
Instances of operation hold coupled instances of `RequestTemplate` and
`ResponseReader`, making the relationship between request types
(`RequestTemplate`) and response types (`ResponseReader`) explicit.

For each operation defined in the OpenAPI specification for a service, there is
a corresponding `Operations.operationName()` function that returns the operation
instance. This arrangement should make it easy for users to discover operations
with their IDE's code completion prompts.

```java
Operations.createPet(); // => Operation<CreatePetResponse>
```

### `RequestTemplate` and `IParameters` - Bind Request Parameters

`RequestTemplate`s help the user create a request using the domain language of a
particular OpenAPI operation. The `RequestTemplate` is a record whose components
are concrete implementations of `IPathParameters`, `IQueryParameters`,
`IHeaderParameters`, `ICookieParameters`, and `IBodyParameters`. Those
`IParameters` interfaces present an API for binding parameters to requests based
on their "location" in the HTTP request (e.g. in the path or the query) and
their name in the OpenAPI specification.

```
var getPetTemplate = Operations.getPet().requestTemplate();
getPetTemplate
  .withPathParameters(p -> p.withFoo("foo"))
  .withQueryParameters(p -> p.withBar("bar"))
  .withCookieParameters(p -> p.withBaz("baz"))
  .withHeaderParameters(p -> p.withBiff("biff"))
  .withBodyParameters(p -> p.withBang("bang"));
  // => an immutable copy with the requested modifications
```

After parameters are bound, they may be retrieved through a set of accessors:

```java
// The generated model (DTO) with bound parameters, e.g. CreatePetBody
template.body();

template.headerParameters();     // => IHeaderParameters
template.cookieParameters();     // => ICookieParameters
template.queryParameters();      // => IQueryParameters
template.pathParameters();       // => IPathParameters
```

Templates are **immutable** so that they can be shared and re-used throughout an
application.

Templates are **logic-less** data containers. The knowledge of how to convert a
template into an HttpRequest is intentionally kept separate from the data
itself. This forces the API to expose all the data that could be required to
make an HTTP, which ensures the API is flexible enough to be used for new
purposes (such as to implement HTTP requests with a different HTTP library than 
`java.net.http`.)

#### Template Overrides

TODO: Consider a set of override methods like the following. They take
precedence over the IParameters bindings and give users a way to "fix" malformed
specifications. This may be a useful way to work around flawed specifications,
but there could be higher-leverage ways to achieve the same goal (such as
"mix-in" specifications that add/remove from a "base"/"source-of-truth"
specification.)

```java
myTemplate
    .withPathOverride("/my/custom/path")
    .withQueryOverride("?my=custom,query,fragment")
    .withCookieOverride(Map.of(
        "Cookie", List.of("Version=\"1\"", "cats=\"dogs,skunks,mice\"")))
    .withHeaderOverride(Map.of(
        "x-header-name", List.of("x-header-value"),
        "biff-header", List.of("biff")))
    .withBodyOverride(myJsonPayload.getBytes()); // Any byte array

template.pathOverride();   // => Optional<String>
template.queryOverride();  // => Optional<String>
template.cookieOverride(); // => Map<String, List<String>> of cookies
template.headerOverride(); // => Map<String, List<String>>
template.bodyOverride();   // => byte[]
```

#### Optional Validation

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

#### TODO: Cookies

This section impacts how we convert an `ICookieParameters` to a map so that we
can configure an `HttpRequest`.

The java.net.http cookie management API renders `$` prefixes for cookie
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

## HTTP Layer API

The HTTP layer consists of:

- The `java.net.http` package.
- `HttpRequests`
- `HttpResponses`

If a user is unable to create a request with the domain layer or can't work
around a bug with [template overrides](#template-overrides), they can shift
from the domain layer to the Http layer.
 
Typically, the user will either need to customize how requests are made,
responses are read, or both.

* To customize requests, the user can first populate a template, then use
  `HttpRequests.builderFrom(String baseUrl, ITemplate template)` to convert it
  to an HTTP request builder. They can then use any of the builder APIs to
  manipulate the request and send it with an HTTP client.
* To customize response deserialization, the user can define their own model
  with Jackson annotations and use an object mapper to read it from an
  `HttpResponse`.

### `HttpRequests` - Convert `ITemplate`s Into `HttpRequest`s

The `HttpRequests` class converts templates with bound parameters and/or 
overrides into `java.net.http.HttpRequest.Builder`s, which let users customize
the request arbitrarily:

```java
HttpRequests.builder("https://example.com/base/url", template);
// => java.net.http.HttpRequest$Builder
```

Note the example URL parameter is the base URL for the service. Template paths
are resolved _relative_ to this base URL.

### `HttpResponses` - Convert `HttpResponse`s Into Domain Types

The `HttpResponses` class converts `java.net.http.HttpResponse` objects into
any of the Lily-generated domain-layer models, e.g. `CreatePetResponse`:

```java
// Creates an instance of CreatePetResponse (like CreatePet200) from the 
// response object:
HttpResponses.read(httpResponse, CreatePetResponse.class);
// Caution: if you ask it to read the wrong response class, this will fail!
```

### TODO

Taken together, the user can write a bespoke request with these tools that 
takes advantage of as much of the generated API as possible:

```java
var template = Directory.createPet()
    // (Bind whatever seems to work here)
    ;
var httpRequest = HttpRequests.builder("https://example.com/", template)
    // (Override the native HTTP request here)
    ;

var httpResponse = HttpClient.newHttpClient().send(
    httpRequest.build(),
    BodyHandlers.ofByteArray());
// => A java.net.http.HttpResponse object


// Read a domain-layer response if possible:
switch (HttpResponses.read(httpResponse, CreatePetResponse.class)) {
    case CreatePet200 ok -> ok.body();
    // other cases here. Exhaustively type-checked.
}

// ...or deserialize the httpResponse manually:
switch (httpResponse.statusCode()) {
    case 200: return JACKSON_MAPPER.readValue(response.body(), MyDto.class);
    // other cases here.
}

```

### Customization

OpenAPI specifications may be flawed or incomplete, created flawed and
incomplete generated code. There are several ways to work around flaws without
leaving the Lily API.

The first is to use [template overrides](#template-overrides), which we've
already discussed.

The second is to create our own templates and exchange, assuming there are only
a few things we need to "fix" about the generated code. For example, assume the
`getPet` operation is missing a path parameter, `id`. We can use subclassing to
add it in:

```java
class MyTemplate extends GetPetTemplate {
  // Not an `@Overrides`, technically an overload.
  MyTemplate withPathParameters(Function<MyPathParameters, MyPathParameters> f) {
    
  }
  
  class MyPathParameters extends GetPetTemplate.PathParameters {
    String myCustomField;
    
    MyPathParameters withMyCustomField(String myCustomField) {
      this.myCustomField = myCustomField;
      return this;
    }
    
    // Other fields and builders are defined by GetPetTemplate.PathParameters
  }
  
  // ITemplate overrides, e.g:
  @Override
  Map<String, Object> pathParameters() {
    var m = new HashMap<>();
    m.putAll(delegate.pathParameters()); // 
  }
}
```

### TODO: De/Serialization Customization

The `Api` exposes an alternative signature to let users customize the
serialization library:

```java
Api.sendSync(
    HttpClient.newHttpClient(),
    new ObjectMapper(), // for request serialization
    new ObjectMapper(), // for response deserialization(?; see TODO)
    "https://example.com",
    Directory.getPet().withPathParameters(p -> p.withId("foo")));
// => a GetPetResponse, like GetPet200, GetPet404, or GetPetUnexpectedCode
```

TODO: I am unsure if we need two or if just one would be sufficient. Are
configurations for serialization separate from deserialization? I.e. can
dates be _written_ one way and _read_ another?

TODO: The http client and object mapper(s) could be collapsed into a single
"options" object.

The user may provide a custom ObjectMapper to configure de/serialization,
which may let them fix certain classes of error.


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

TODO: response sum type names and response body models are both subject to 
the problem of name changes over time, and there isn't an obvious way to 
avoid coupling to them. Make a note about that in this section.

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