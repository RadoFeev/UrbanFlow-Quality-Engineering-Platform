package io.urbanflow.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.urbanflow.identity.domain.Permission;
import io.urbanflow.identity.domain.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BearerTokenFilterTest {

  private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

  @Mock private AccessTokenService accessTokenService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void installsVerifiedPrincipalAndRoleAuthorities() throws Exception {
    BearerTokenFilter filter = filter();
    AccessTokenClaims claims = claims();
    when(accessTokenService.verify("valid-token", NOW)).thenReturn(claims);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (servletRequest, servletResponse) -> {
          assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
              .isEqualTo(claims);
          assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
              .extracting(Object::toString)
              .contains("ROLE_CUSTOMER", Permission.SHIPMENT_CREATE.name());
        });

    verify(accessTokenService).verify("valid-token", NOW);
  }

  @Test
  void marksInvalidBearerAndContinuesUnauthenticated() throws Exception {
    BearerTokenFilter filter = filter();
    when(accessTokenService.verify("invalid-token", NOW))
        .thenThrow(new TokenValidationException("signature invalid"));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (servletRequest, servletResponse) ->
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());

    assertThat(request.getAttribute(BearerTokenFilter.TOKEN_FAILURE_ATTRIBUTE))
        .isEqualTo("signature invalid");
  }

  @Test
  void ignoresRequestWithoutBearerCredential() throws Exception {
    BearerTokenFilter filter = filter();

    filter.doFilter(
        new MockHttpServletRequest("GET", "/actuator/health"),
        new MockHttpServletResponse(),
        (servletRequest, servletResponse) -> {});

    verifyNoInteractions(accessTokenService);
  }

  private BearerTokenFilter filter() {
    return new BearerTokenFilter(accessTokenService, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static AccessTokenClaims claims() {
    return new AccessTokenClaims(
        UUID.fromString("3f3ddce0-b78a-4eb3-837b-53e7a5297930"),
        "customer@example.test",
        Role.CUSTOMER,
        Set.copyOf(Role.CUSTOMER.permissions()),
        UUID.fromString("b95973e4-b6d8-44d4-b5da-c095014dbdf5"),
        NOW,
        NOW.plusSeconds(900));
  }
}
