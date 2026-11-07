package com.gamerecruitment.account.adapter.web;

import com.gamerecruitment.account.adapter.persistence.TransactionalRegistration;
import com.gamerecruitment.account.adapter.web.generated.api.AccountsApi;
import com.gamerecruitment.account.adapter.web.generated.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RegistrationController implements AccountsApi {
  private final TransactionalRegistration registration;

  public RegistrationController(TransactionalRegistration registration) {
    this.registration = registration;
  }

  public ResponseEntity<Account> registerAccount(RegisterRequest request) {
    var a =
        registration.execute(request.getLoginId(), request.getPassword(), request.getDisplayName());
    return ResponseEntity.status(201).body(new Account(a.id(), a.displayName()));
  }
}
