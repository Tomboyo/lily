# 1. Functional GAPI

Date: 2025-02-28

## Status

Accepted

## Context

The object-oriented generated API (GAPI) creates opportunities for name
collision that can only be avoided with callback namespaces or name
prefixes/suffixes. This clutters the API. This is always an issue when an object
holds both data and methods, since we want to expose both to allow the user to
work around shortcomings in the GAPI. For example, the GAPI Api class exposes
the ObjectMapper as `.objectMapper()`, but if there is a tag called
`objectMapper` in the specification, we risk a collision when we generate an
operation group, hence the suffixed name `.objectMapperOperations()`.

On that note, tags feel like boilerplate, especially to someone familiar with
the API.

## Decision

We will separate data from functionality, giving us the following updated GAPI:

```java
var config = MyApiConfig.newBuilder()
    .baseUrl("https://example.com/foo")
    .sslContext(SSLContext.getDefaultContext())
    .httpClient(HttpClient.newHttpClient())
    .objectMapper(new ObjectMapper())
    .build();

var template = MyApi.createPet(config)
    .query(it -> ...)
    .body(it -> ...)
    .cookie(it -> ...)
    .header(it -> ...);

template.httpRequestBuilder();
template.bodyPublisher();

switch (Lily.send(template)) {
    case CreatePet200 x -> ok(x);
    case CreatePet404 x -> missing(x);
    case CreatePetDefault x -> error(x.httpResponse());
}

// or

config.httpClient()
    .send(
        template.httpRequestBuilder(),
        template.bodyPublisher());
```

## Consequences

- We no longer have tagged operation groups in the GAPI. Every opteration is
  listed directly on the generated MyApi class.
- The user passes around the MyApiConfig record rather than the MyApi. The user
  is able to replace parameters "just in time" as a result, which can be helpful
  in test cases, especially for users that take advantage of functional style.
