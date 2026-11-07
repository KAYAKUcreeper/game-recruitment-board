package com.gamerecruitment.account.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.*;
import org.springframework.stereotype.Component;

@Component
public class CsrfSupport implements CsrfTokenRequestHandler {
  private final CookieCsrfTokenRepository repository =
      CookieCsrfTokenRepository.withHttpOnlyFalse();
  private final XorCsrfTokenRequestAttributeHandler masked =
      new XorCsrfTokenRequestAttributeHandler();

  public CsrfSupport(@Value("${app.secure-cookies:true}") boolean secure) {
    repository.setCookieCustomizer(c -> c.path("/").sameSite("Lax").secure(secure));
  }

  public CookieCsrfTokenRepository repository() {
    return repository;
  }

  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
    masked.handle(request, response, token);
    request.setAttribute("rawCsrfToken", token.get());
  }

  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
    // The contract requires the raw header; parameters cannot substitute for it.
    return request.getHeader(token.getHeaderName());
  }

  public CsrfToken current(HttpServletRequest request) {
    return (CsrfToken) request.getAttribute("rawCsrfToken");
  }

  public void clear(HttpServletRequest request, HttpServletResponse response) {
    repository.saveToken(null, request, response);
  }
}
