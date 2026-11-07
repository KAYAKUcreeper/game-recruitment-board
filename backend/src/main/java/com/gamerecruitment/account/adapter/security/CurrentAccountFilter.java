package com.gamerecruitment.account.adapter.security;

import com.gamerecruitment.account.adapter.web.ApiExceptionHandler;
import com.gamerecruitment.account.application.AccountFailure;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class CurrentAccountFilter extends OncePerRequestFilter {
  private final CurrentAccountAuthenticator authenticator;
  private final CookieTokenResolver resolver;
  private final ObjectMapper mapper;

  public CurrentAccountFilter(
      CurrentAccountAuthenticator authenticator,
      CookieTokenResolver resolver,
      ObjectMapper mapper) {
    this.authenticator = authenticator;
    this.resolver = resolver;
    this.mapper = mapper;
  }

  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/api/auth/me".equals(request.getRequestURI());
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    CurrentAccountAuthenticator.Current current;
    try {
      current = authenticator.required(resolver.resolve(request));
    } catch (AccountFailure e) {
      error(response, 401, "UNAUTHENTICATED");
      return;
    } catch (DataAccessException e) {
      error(response, 503, "SERVICE_UNAVAILABLE");
      return;
    }
    request.setAttribute("currentAccount", current);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(current.account().id(), null, List.of()));
    SecurityContextHolder.setContext(context);
    chain.doFilter(request, response);
  }

  private void error(HttpServletResponse response, int status, String code) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    mapper.writeValue(response.getOutputStream(), ApiExceptionHandler.body(code, List.of()));
  }
}
