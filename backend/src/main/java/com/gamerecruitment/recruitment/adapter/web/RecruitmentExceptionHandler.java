package com.gamerecruitment.recruitment.adapter.web;

import com.gamerecruitment.recruitment.application.RecruitmentFailure;
import com.gamerecruitment.shared.adapter.web.ApiErrorBody;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-1)
public class RecruitmentExceptionHandler {
  @ExceptionHandler(RecruitmentFailure.class)
  ResponseEntity<ApiErrorBody> recruitment(RecruitmentFailure failure) {
    int status =
        switch (failure.code()) {
          case VALIDATION_ERROR -> 400;
          case NOT_OWNER, MEETING_INFO_FORBIDDEN -> 403;
          case RECRUITMENT_NOT_FOUND -> 404;
          default -> 409;
        };
    return ResponseEntity.status(status)
        .body(
            new ApiErrorBody(
                failure.code().name(), message(failure.code()), failure.fields()));
  }

  private String message(RecruitmentFailure.Code code) {
    return switch (code) {
      case VALIDATION_ERROR -> "入力項目を確認してください。";
      case NOT_OWNER -> "募集者本人だけが操作できます。";
      case MEETING_INFO_FORBIDDEN -> "合流情報を閲覧できません。";
      case RECRUITMENT_NOT_FOUND -> "募集が見つかりません。";
      case OWN_RECRUITMENT -> "自分の募集には参加できません。";
      case ALREADY_PARTICIPATING -> "すでに参加しています。";
      case NOT_PARTICIPATING -> "参加中ではありません。";
      case RECRUITMENT_FULL -> "募集は満員です。";
      case RECRUITMENT_CLOSED -> "募集は締め切られています。";
      case RECRUITMENT_CANCELLED -> "募集は中止されています。";
      case RECRUITMENT_STARTED -> "募集は開始済みです。";
      case NOT_MANUALLY_CLOSED -> "手動締め切り中の募集ではありません。";
    };
  }
}
