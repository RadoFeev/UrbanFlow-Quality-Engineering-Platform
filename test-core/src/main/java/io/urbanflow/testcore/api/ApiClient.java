package io.urbanflow.testcore.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Objects;

/** Thin REST Assured client that centralizes specifications and authentication. */
public final class ApiClient {

  private final RequestSpecification specification;
  private final String bearerToken;

  /** Creates an unauthenticated client. */
  public ApiClient(RequestSpecification specification) {
    this(specification, null);
  }

  private ApiClient(RequestSpecification specification, String bearerToken) {
    this.specification = Objects.requireNonNull(specification);
    this.bearerToken = bearerToken;
  }

  /** Returns a new client that adds a bearer credential to every request. */
  public ApiClient authenticated(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("bearer token must not be blank");
    }
    return new ApiClient(specification, token.trim());
  }

  /** Executes a GET request. */
  public Response get(String path, Object... pathParameters) {
    return request().get(path, pathParameters);
  }

  /** Executes a POST request with a JSON body. */
  public Response post(String path, Object body, Object... pathParameters) {
    return request().body(body).post(path, pathParameters);
  }

  /** Executes a PUT request with a JSON body. */
  public Response put(String path, Object body, Object... pathParameters) {
    return request().body(body).put(path, pathParameters);
  }

  /** Executes a PATCH request with a JSON body. */
  public Response patch(String path, Object body, Object... pathParameters) {
    return request().body(body).patch(path, pathParameters);
  }

  /** Executes a DELETE request. */
  public Response delete(String path, Object... pathParameters) {
    return request().delete(path, pathParameters);
  }

  /** Creates an independent request from the immutable base specification. */
  public RequestSpecification request() {
    RequestSpecification request = RestAssured.given().spec(specification);
    return bearerToken == null ? request : request.auth().oauth2(bearerToken);
  }
}
