package io.urbanflow.identity.security;

import io.urbanflow.identity.config.IdentityWebProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless least-privilege HTTP security rules for identity endpoints. */
@Configuration
public class SecurityConfiguration {

  /** Configures public authentication endpoints and protected user administration. */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      BearerTokenFilter bearerTokenFilter,
      SecurityErrorResponder securityErrorResponder)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(securityErrorResponder)
                    .accessDeniedHandler(securityErrorResponder))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/users/me")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/role")
                    .hasAuthority("ROLE_MANAGE")
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** Restricts browser access to configured portals and required API headers/methods. */
  @Bean
  CorsConfigurationSource corsConfigurationSource(IdentityWebProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.IF_MATCH,
            "X-Correlation-ID",
            "traceparent"));
    configuration.setExposedHeaders(List.of(HttpHeaders.ETAG, "X-Correlation-ID", "X-Trace-ID"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
