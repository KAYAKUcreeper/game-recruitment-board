package com.gamerecruitment.account.adapter.web;

import com.gamerecruitment.account.adapter.web.generated.model.Error;
import com.gamerecruitment.account.application.AccountFailure;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  public static Error body(String code, List<String> fields) {
    String message =
        switch (code) {
          case "VALIDATION_ERROR" -> "入力項目の形式・文字数を確認してください。";
          case "LOGIN_ID_TAKEN" -> "このログインIDは使用されています。";
          case "INVALID_CREDENTIALS" -> "ログインIDまたはパスワードが一致しません。";
          case "UNAUTHENTICATED" -> "ログインしてください。";
          case "ALREADY_AUTHENTICATED" -> "先にログアウトしてください。";
          case "CSRF_INVALID" -> "画面を更新して、もう一度操作してください。";
          case "METHOD_NOT_ALLOWED" -> "この操作方法は利用できません。";
          case "SERVICE_UNAVAILABLE" -> "現在処理できません。時間をおいて再度お試しください。";
          default -> "処理を完了できませんでした。";
        };
    return new Error(
        Error.CodeEnum.fromValue(code),
        message,
        fields.stream()
            .map(Error.FieldsEnum::fromValue)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @ExceptionHandler(AccountFailure.class)
  ResponseEntity<Error> account(AccountFailure e) {
    int status =
        switch (e.code()) {
          case VALIDATION_ERROR -> 400;
          case UNAUTHENTICATED, INVALID_CREDENTIALS -> 401;
          default -> 409;
        };
    return ResponseEntity.status(status).body(body(e.code().name(), e.fields()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Error> json() {
    return ResponseEntity.badRequest().body(body("VALIDATION_ERROR", List.of()));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Error> media() {
    return ResponseEntity.status(415).body(body("UNSUPPORTED_MEDIA_TYPE", List.of()));
  }

  @ExceptionHandler({DataAccessException.class, TransactionException.class})
  ResponseEntity<Error> database() {
    return ResponseEntity.status(503).body(body("SERVICE_UNAVAILABLE", List.of()));
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Error> method(
      org.springframework.web.HttpRequestMethodNotSupportedException exception) {
    var builder = ResponseEntity.status(405);
    if (exception.getSupportedHttpMethods() != null)
      builder.allow(
          exception.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new));
    return builder.body(body("METHOD_NOT_ALLOWED", List.of()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Error> unexpected(Exception exception) {
    org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
        .warn("Unhandled API exception type: {}", exception.getClass().getName());
    return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR", List.of()));
  }
}
