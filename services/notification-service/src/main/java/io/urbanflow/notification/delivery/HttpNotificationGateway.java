package io.urbanflow.notification.delivery;

import io.urbanflow.notification.domain.Notification;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTPS adapter for the internal communications gateway. */
@Component
public class HttpNotificationGateway implements NotificationGateway {

  private final RestClient restClient;

  /** Creates the gateway adapter from the bounded shared client. */
  public HttpNotificationGateway(RestClient notificationRestClient) {
    this.restClient = notificationRestClient;
  }

  @Override
  public String deliver(Notification notification) {
    GatewayResponse response =
        restClient
            .post()
            .uri("/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", notification.id().toString())
            .body(
                new GatewayRequest(
                    notification.customerId(),
                    notification.type(),
                    notification.subject(),
                    notification.body()))
            .retrieve()
            .body(GatewayResponse.class);
    if (response == null || response.messageId() == null || response.messageId().isBlank()) {
      throw new NotificationGatewayException("gateway response is missing messageId");
    }
    return response.messageId();
  }

  private record GatewayRequest(UUID customerId, String type, String subject, String body) {}

  private record GatewayResponse(String messageId) {}
}
