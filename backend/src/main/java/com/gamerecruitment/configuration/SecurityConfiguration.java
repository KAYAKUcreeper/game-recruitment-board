package com.gamerecruitment.configuration;

import com.gamerecruitment.account.adapter.security.*;
import com.gamerecruitment.account.adapter.web.ApiExceptionHandler;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfiguration {
  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      CsrfSupport csrf,
      ObjectMapper mapper,
      @Value("${app.allowed-origin}") String origin,
      CurrentAccountAuthenticator authenticator,
      CookieTokenResolver resolver)
      throws Exception {
    return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/me")
                    .authenticated()
                    .requestMatchers(
                        "/api/accounts", "/api/auth/csrf", "/api/auth/login", "/api/auth/logout")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .csrf(c -> c.csrfTokenRepository(csrf.repository()).csrfTokenRequestHandler(csrf))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (req, res, ex) -> {
                      res.setStatus(403);
                      res.setContentType("application/json");
                      mapper.writeValue(
                          res.getOutputStream(),
                          ApiExceptionHandler.body("CSRF_INVALID", List.of()));
                    }))
        .addFilterBefore(new ApiBoundaryFilter(origin, mapper), CsrfFilter.class)
        .addFilterBefore(
            new CurrentAccountFilter(authenticator, resolver, mapper),
            org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
        .build();
  }
}
