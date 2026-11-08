package io.github.tomboyo.lily.http.model;

import java.util.function.Function;

public record ParameterBindings<
    Path extends IParameters,
    Query extends IParameters,
    Header extends IParameters,
    Cookie extends IParameters,
    Body>(
    Path pathParameters,
    Query queryParameters,
    Header headerParameters,
    Cookie cookieParameters,
    Body bodyParameters) {

  /* Binding functions -- assign values to parameters */

  public ParameterBindings<Path, Query, Header, Cookie, Body> bindPathParameters(
      Function<Path, Path> f) {
    return new ParameterBindings<>(
        f.apply(pathParameters),
        queryParameters,
        headerParameters,
        cookieParameters,
        bodyParameters);
  }

  public ParameterBindings<Path, Query, Header, Cookie, Body> bindQueryParameters(
      Function<Query, Query> f) {
    return new ParameterBindings<>(
        pathParameters,
        f.apply(queryParameters),
        headerParameters,
        cookieParameters,
        bodyParameters);
  }

  public ParameterBindings<Path, Query, Header, Cookie, Body> bindHeaderParameters(
      Function<Header, Header> f) {
    return new ParameterBindings<>(
        pathParameters,
        queryParameters,
        f.apply(headerParameters),
        cookieParameters,
        bodyParameters);
  }

  public ParameterBindings<Path, Query, Header, Cookie, Body> bindCookieParameters(
      Function<Cookie, Cookie> f) {
    return new ParameterBindings<>(
        pathParameters,
        queryParameters,
        headerParameters,
        f.apply(cookieParameters),
        bodyParameters);
  }

  public ParameterBindings<Path, Query, Header, Cookie, Body> bindBodyParameters(
      Function<Body, Body> f) {
    return new ParameterBindings<>(
        pathParameters,
        queryParameters,
        headerParameters,
        cookieParameters,
        f.apply(bodyParameters));
  }

  /* Wither functions -- change the types of parameters.  */

  public <Path2 extends IParameters>
      ParameterBindings<Path2, Query, Header, Cookie, Body> withPathParameters(
          Path2 pathParameters) {
    return new ParameterBindings<>(
        pathParameters, queryParameters, headerParameters, cookieParameters, bodyParameters);
  }

  public <Query2 extends IParameters>
      ParameterBindings<Path, Query2, Header, Cookie, Body> withQueryParameters(
          Query2 queryParameters) {
    return new ParameterBindings<>(
        pathParameters, queryParameters, headerParameters, cookieParameters, bodyParameters);
  }

  public <Header2 extends IParameters>
      ParameterBindings<Path, Query, Header2, Cookie, Body> withHeaderParameters(
          Header2 headerParameters) {
    return new ParameterBindings<>(
        pathParameters, queryParameters, headerParameters, cookieParameters, bodyParameters);
  }

  public <Cookie2 extends IParameters>
      ParameterBindings<Path, Query, Header, Cookie2, Body> withCookieParameters(
          Cookie2 cookieParameters) {
    return new ParameterBindings<>(
        pathParameters, queryParameters, headerParameters, cookieParameters, bodyParameters);
  }

  public <Body2> ParameterBindings<Path, Query, Header, Cookie, Body2> withBodyParameters(
      Body2 bodyParameters) {
    return new ParameterBindings<>(
        pathParameters, queryParameters, headerParameters, cookieParameters, bodyParameters);
  }
}
