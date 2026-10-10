package com.gamerecruitment.account.application.port;

public interface PasswordHasher {
  String hash(String password);

  boolean matches(String password, String hash);
}
