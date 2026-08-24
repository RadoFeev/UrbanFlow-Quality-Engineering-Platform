package io.urbanflow.contract;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

final class PactHttpClient {

  private final HttpClient client =
      HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

  Response exchange(
      String baseUrl, String method, String path, String body, Map<String, String> headers) {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path));
    headers.forEach(request::header);
    if (body == null) {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      request.method(method, HttpRequest.BodyPublishers.ofString(body));
    }
    try {
      HttpResponse<String> response =
          client.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body(), response.headers().map());
    } catch (IOException exception) {
      throw new PactClientException("Pact mock request failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PactClientException("Pact mock request was interrupted", exception);
    }
  }

  record Response(int status, String body, Map<String, java.util.List<String>> headers) {}

  static final class PactClientException extends RuntimeException {

    PactClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
