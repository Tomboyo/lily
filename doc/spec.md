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

## Templates

Each operation described by an OpenAPI specification is compiled into a
_template_. Templates may be created using the template directory, `Directory`.

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
    .withPathOverride("/my/custom/path")
    .withQueryOverride("?my=custom,query,fragment")
    .withCookieOverride(Map.of(
        "Cookie", List.of("Version=\"1\"", "cats=\"dogs,skunks,mice\"")))
    .withHeaderOverride(Map.of(
        "x-header-name", List.of("x-header-value"),
        "biff-header", List.of("biff")))
    .withBodyOverride(myJsonPayload.getBytes()); // Any byte array
```

Templates are immutable so that they can be easily shared. For each of the
withers described above, the bound parameters can be retrieved with a
corresponding record-style getter:

```java
template.pathParameters().baz();    // => "baz"
template.qeryParameters().foo();    //=> "foo"
template.cookieParameters().bar();  // => "bar"
template.headerParameters().biff(); // => "biff"
// The generated model (DTO) with bound parameters, e.g. CreatePetBody
template.body();

template.pathOverride();   // => Optional<String>
template.queryOverride();  // => Optional<String>
template.cookieOverride(); // => Map<String, List<String>> of cookies
template.headerOverride(); // => Map<String, List<String>>
template.bodyOverride();   // => A byte array.
```

Templates are logic-less data containers. The knowledge of how to convert a 
template into an HttpRequest is intentionally kept separate from the data
itself: If our generated code is able to use the template api to produce HTTP
requests, an end-user probably can as well (whereas if the templates 
contained the logic, we might accidentally make crucial data private or hard
to access). This keeps the generated code flexible enough to support e.g. the
nuclear scenario where a user wants to use a totally different http library.

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

## Making Requests And Reading Responses

The `Api` class provided the most convenient method for sending requests and 
parsing responses. Given a client, base-url, and a populated template, `Api` 
will issue an HTTP request and convert the response to an instance of a 
sealed interface corresponding to the documented HTTP return codes:

```java
var response = Api.sendSync(
        HttpClient.newHttpClient(),
        "https://example.com",
        Directory.getPet().withPathParameters(p -> p.withId("foo")));
// => a GetPetResponse, like GetPet200, GetPet404, or GetPetUnexpectedCode
```

This is intended to satisfy the common use-case, but only works when the 
specification is accurate. Users can access less-abstract levels of the 
generated API to work around flaws in the specification, described in the 
following subsections.

### De/Serialization Customization

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

The user may provide a custom ObjectMapper to configure de/serialization, 
which may let them fix certain classes of error.

### Template Overrides

Recall that if the specification contains flaws, the user may be able to
work around them using the request template's `with*Override` methods to
directly configure path, query, cookie, header, or request bodies (see
[Templates](#Templates)). This may be powerful enough for the user, and keeps
them insulated from lower-level details of request/response generation.

### Resort To java.net.http API 

In the worst case, users can convert templates to
`java.net.http.HttpRequest$Builder`s, make requests with an `HttpClient` 
manually, and either handle the response with bespoke code or convert it to 
a generated response object.

The `HttpRequests` class converts templates with bound parameters and/or 
overrides into `java.net.http.HttpRequest.Builder`s:

```java
HttpRequests.builder(template) // => java.net.http.HttpRequest$Builder
    .build();                  //  => java.net.http.HttpRequest
```

And similarly, the `HttpResponses` class converts `java.net.http.
HttpResponse` objects into generated response objects (e.g. 
a member of the `CreatePetResponse` sealed interface):

```java
// Creates an instance of CreatePetResponse (like CreatePet200) from the 
// response object:
HttpResponses.read(httpResponse, CreatePetResponse.class);
// Caution: if you ask it to read the wrong response class, this will fail!
```

Taken together, the user can write a bespoke request with these tools that 
takes advantage of as much of the generated API as possible:

```java
var template = Directory.createPet()
    // (Bind whatever seems to work here)
    ;
var httpRequest = HttpRequests.builder(template)
    // (Override the native HTTP request here)
    ;

var httpResponse = HttpClient.newHttpClient().send(
    httpRequest.build(),
    BodyHandlers.ofByteArray());
// => A java.net.http.HttpResponse object

// Either inspect and deserialize the httpResponse manually in the usual way:
switch (httpResponse.statusCode()) {
    case 200: return JACKSON_MAPPER.readValue(response.body(), MyDto.class);
    // other cases here.
}

// ... or use the generated API to deserialize it to a response object:
switch (HttpResponses.read(httpResponse, CreatePetResponse.class)) {
    case CreatePet200 ok -> ok.body();
    // other cases here. Exhaustively type-checked.
}
```

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