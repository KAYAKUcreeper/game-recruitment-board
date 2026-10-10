package com.gamerecruitment.account.adapter.web;

import com.gamerecruitment.account.adapter.persistence.TransactionalLogout;
import com.gamerecruitment.account.adapter.security.*;
import com.gamerecruitment.account.adapter.web.generated.api.AuthenticationApi;
import com.gamerecruitment.account.adapter.web.generated.model.*;
import com.gamerecruitment.account.application.LoginAccount;
import jakarta.servlet.http.*;
import java.time.ZoneOffset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthenticationController implements AuthenticationApi {
  private final LoginAccount login;
  private final TransactionalLogout logout;
  private final CurrentAccountAuthenticator authenticator;
  private final CookieTokenResolver resolver;
  private final AuthCookieWriter cookies;
  private final CsrfSupport csrf;
  private final CsrfController csrfController;
  private final HttpServletRequest request;
  private final HttpServletResponse response;

  public AuthenticationController(
      LoginAccount login,
      TransactionalLogout logout,
      CurrentAccountAuthenticator authenticator,
      CookieTokenResolver resolver,
      AuthCookieWriter cookies,
      CsrfSupport csrf,
      CsrfController csrfController,
      HttpServletRequest request,
      HttpServletResponse response) {
    this.login = login;
    this.logout = logout;
    this.authenticator = authenticator;
    this.resolver = resolver;
    this.cookies = cookies;
    this.csrf = csrf;
    this.csrfController = csrfController;
    this.request = request;
    this.response = response;
  }

  public ResponseEntity<CsrfToken> getCsrfToken() {
    return csrfController.get(request);
  }

  public ResponseEntity<LoginState> getCurrentAccount() {
    var current = (CurrentAccountAuthenticator.Current) request.getAttribute("currentAccount");
    return ResponseEntity.ok(
        new LoginState(
            new Account(current.account().id(), current.account().displayName()),
            current.token().expiresAt().atOffset(ZoneOffset.UTC)));
  }

  public ResponseEntity<LoginState> login(LoginRequest input) {
    var result =
        login.execute(
            input.getLoginId(),
            input.getPassword(),
            authenticator.optional(resolver.resolve(request)).isPresent());
    cookies.set(response, result.token().value());
    csrf.clear(request, response);
    return ResponseEntity.ok(
        new LoginState(
            new Account(result.account().id(), result.account().displayName()),
            result.token().details().expiresAt().atOffset(ZoneOffset.UTC)));
  }

  public ResponseEntity<Void> logout() {
    logout.execute(resolver.resolve(request));
    cookies.clear(response);
    csrf.clear(request, response);
    return ResponseEntity.noContent().build();
  }
}
