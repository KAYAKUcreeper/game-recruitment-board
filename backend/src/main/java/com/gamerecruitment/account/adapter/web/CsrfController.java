package com.gamerecruitment.account.adapter.web;

import com.gamerecruitment.account.adapter.security.CsrfSupport;
import com.gamerecruitment.account.adapter.web.generated.model.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Handles the CSRF operation delegated by the generated AuthenticationApi implementation. */
@Component
public class CsrfController {
  private final CsrfSupport support;

  public CsrfController(CsrfSupport support) {
    this.support = support;
  }

  public ResponseEntity<CsrfToken> get(HttpServletRequest request) {
    var token = support.current(request);
    return ResponseEntity.ok(
        new CsrfToken(token.getToken(), CsrfToken.HeaderNameEnum.X_XSRF_TOKEN));
  }
}
