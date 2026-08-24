package io.urbanflow.identity.api;

import io.urbanflow.identity.application.UserAdministrationService;
import io.urbanflow.identity.application.UserProfile;
import io.urbanflow.identity.security.AccessTokenClaims;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated profile and permission-gated role administration API. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserAdministrationService users;

  /** Creates the authenticated user HTTP adapter. */
  public UserController(UserAdministrationService users) {
    this.users = users;
  }

  /** Returns the current token subject profile. */
  @GetMapping("/me")
  public ResponseEntity<UserProfile> me(@AuthenticationPrincipal AccessTokenClaims claims) {
    return profileResponse(users.profile(claims.userId()));
  }

  /** Changes a role using a mandatory strong entity tag for optimistic concurrency. */
  @PatchMapping("/{id}/role")
  public ResponseEntity<UserProfile> changeRole(
      @PathVariable UUID id,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @Valid @RequestBody RoleUpdateRequest request) {
    return profileResponse(users.changeRole(id, request.role(), EntityTagVersion.parse(ifMatch)));
  }

  private static ResponseEntity<UserProfile> profileResponse(UserProfile profile) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .eTag(EntityTagVersion.format(profile.version()))
        .body(profile);
  }
}
