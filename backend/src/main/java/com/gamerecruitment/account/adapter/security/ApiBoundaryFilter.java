package com.gamerecruitment.account.adapter.security;

import com.gamerecruitment.account.adapter.web.ApiExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class ApiBoundaryFilter extends OncePerRequestFilter {
  private final String origin;
  private final ObjectMapper mapper;

  public ApiBoundaryFilter(String origin, ObjectMapper mapper) {
    this.origin = origin;
    this.mapper = mapper;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    response.setHeader("Cache-Control", "no-store");
    if ("POST".equals(request.getMethod())
        && request.getHeader("Origin") != null
        && !origin.equals(request.getHeader("Origin"))) {
      response.setStatus(403);
      response.setContentType("application/json");
      mapper.writeValue(
          response.getOutputStream(), ApiExceptionHandler.body("CSRF_INVALID", List.of()));
      return;
    }
    chain.doFilter(request, response);
  }
}
