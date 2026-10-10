package com.gamerecruitment.account.application;

import java.util.List;

public class AccountFailure extends RuntimeException {
  public enum Code {
    VALIDATION_ERROR,
    UNAUTHENTICATED,
    INVALID_CREDENTIALS,
    LOGIN_ID_TAKEN,
    ALREADY_AUTHENTICATED
  }

  private final Code code;
  private final List<String> fields;

  public AccountFailure(Code code) {
    this(code, List.of());
  }

  public AccountFailure(Code code, List<String> fields) {
    super(code.name());
    this.code = code;
    this.fields = List.copyOf(fields);
  }

  public Code code() {
    return code;
  }

  public List<String> fields() {
    return fields;
  }
}
