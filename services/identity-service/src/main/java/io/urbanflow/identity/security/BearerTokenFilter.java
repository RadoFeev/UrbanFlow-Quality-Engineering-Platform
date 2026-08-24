package io.urbanflow.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Converts a verified bearer token into a Spring Security authentication context. */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

  public static final String TOKEN_FAILURE_ATTRIBUTE =
      BearerTokenFilter.class.getName() + ".failure";

  private final AccessTokenService accessTokenService;
  private final Clock clock;

  /** Creates a bearer filter with an injectable clock. */
  public BearerTokenFilter(AccessTokenService accessTokenService, Clock clock) {
    this.accessTokenService = accessTokenService;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && authorization.startsWith("Bearer ")) {
      try {
        AccessTokenClaims claims =
            accessTokenService.verify(authorization.substring(7), clock.instant());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
        claims.permissions().stream()
            .map(permission -> new SimpleGrantedAuthority(permission.name()))
            .forEach(authorities::add);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(claims, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (TokenValidationException exception) {
        SecurityContextHolder.clearContext();
        request.setAttribute(TOKEN_FAILURE_ATTRIBUTE, exception.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
