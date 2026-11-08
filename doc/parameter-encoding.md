# Parameter Encoding

There are multiple ways to serialize parameters:

1. Using the parameter's schema and style to RFC6570-encode the parameter.
2. Using the parameter's content, which describes one and only one media type.

## Locations

### Path

Path objects define a path template. Serialized parameters are interpolated into
the path. Parameters may be serialized as: [^1]

* matrix (RFC6570 path-style [^1.1] :
  `/foo/{color} -> /foo/;color=R,100,G,200,B,150` (depending on `explode`
  keyword and data type).
* label: `/foo/{color} -> /foo/.R,100,G,200,B,150` (depending on `explode`
  keyword and data type).
* simple: `/foo/R=100,G=200,B=150` (depending on `explode` keyword and data
  type).

Successive path parameters may appear in a template without a separator, as in
`/foo/{color}{name}`. It is up to the OAS and service authors to pick a template
and parameter style which clearly delineates parameters.

* matrix (good): `/foo/;color=R,100,G,200,B,150;name=fred`
* label (bad): `/foo/.R,100,G,200,B,150.fred`
* simple (bad): `/foo/R=100,G=200,B=150name=fred`

### Query

Query fragments begin with `?` and consist of `key={value}`
pairs joined by `&`, as in: `?foo=bar&baz=biff`. Each `{value}` may be
arbitrarily encoded, either by its RFC6570 schema and style, or by its
content. [^3]

* Array parameters may be serialized as
    * `form`: `/foo?color=red,green,blue` or
      `/foo?color=red&color=green&color=blue` depending on the `explode`
      keyword.
    * `spaceDelimited`: `/foo?color=red%20green%20blue`.
    * `pipeDelimited`: `/foo?bar=red|green|blue`.
* Objects may be serialized as
    * `form`: `/foo?color=R,100,G,200,B,150` or `/foo?R=100&G=200&B=150`
      depending on the `explode` keyword.
    * `deepObject`: `/foo?color[R]=100&color[G]=200&color[B]=150`.

### Header

Parameters may be primitive or complex types, but should be encoded with the
`simple` style.

### Cookie

TODO

## Reference Table [^4]

Combinations not in this table are NOT ALLOWED!

| style          | type                     | in            | comments                                                                                                                                                                                                                                                                                                           |
|----------------|--------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| matrix         | primitive, array, object | path          | Path-style parameters defined by RFC6570                                                                                                                                                                                                                                                                           |
| label          | primitive, array, object | path          | Label style parameters defined by RFC6570                                                                                                                                                                                                                                                                          |
| simple         | primitive, array, object | path, header  | Simple style parameters defined by RFC6570. This option replaces collectionFormat with a csv value from OpenAPI 2.0.                                                                                                                                                                                               |
| form           | primitive, array, object | query, cookie | Form style parameters defined by RFC6570. This option replaces collectionFormat with a csv (when explode is false) or multi (when explode is true) value from OpenAPI 2.0.                                                                                                                                         |
| spaceDelimited | array, object            | query         | Space separated array values or object properties and values. This option replaces collectionFormat equal to ssv from OpenAPI 2.0.                                                                                                                                                                                 |
| pipeDelimited  | array, object            | query         | Pipe separated array values or object properties and values. This option replaces collectionFormat equal to pipes from OpenAPI 2.0.                                                                                                                                                                                |
| deepObject     | object                   | query         | Allows objects with scalar properties to be represented using form parameters. The representation of array or object properties is not defined (but see Extending Support for Querystring Formats for alternatives).                                                                                               |
| cookie         | primitive, array, object | cookie        | Analogous to form, but following [[RFC6265]] Cookie syntax rules, meaning that name-value pairs are separated by a semicolon followed by a single space (e.g. n1=v1; n2=v2), and no percent-encoding or other escaping is applied; data values that require any sort of escaping MUST be provided in escaped form. |

[^1]: https://swagger.io/docs/specification/v3_0/describing-parameters/#path-parameters
[^1.1]: https://www.rfc-editor.org/info/rfc6570/#section-3.2.7
[^3]: https://swagger.io/docs/specification/v3_0/describing-parameters/#query-parameters
[^4]: https://swagger.io/specification/#style-values