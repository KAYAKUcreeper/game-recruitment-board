package com.gamerecruitment.recruitment.application;

import java.util.List;

public class RecruitmentFailure extends RuntimeException {
  public enum Code {
    VALIDATION_ERROR,
    NOT_OWNER,
    MEETING_INFO_FORBIDDEN,
    RECRUITMENT_NOT_FOUND,
    OWN_RECRUITMENT,
    ALREADY_PARTICIPATING,
    NOT_PARTICIPATING,
    RECRUITMENT_FULL,
    RECRUITMENT_CLOSED,
    RECRUITMENT_CANCELLED,
    RECRUITMENT_STARTED,
    NOT_MANUALLY_CLOSED
  }

  private final Code code;
  private final List<String> fields;

  public RecruitmentFailure(Code code) {
    this(code, List.of());
  }

  public RecruitmentFailure(Code code, List<String> fields) {
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
