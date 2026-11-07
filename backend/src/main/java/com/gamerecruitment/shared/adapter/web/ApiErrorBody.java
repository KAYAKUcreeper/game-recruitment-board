package com.gamerecruitment.shared.adapter.web;

import java.util.List;

public record ApiErrorBody(String code, String message, List<String> fields) {
  public ApiErrorBody {
    fields = List.copyOf(fields);
  }
}
