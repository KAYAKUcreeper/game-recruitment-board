package com.gamerecruitment.account;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.Application;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {Application.class, ApiTestSupport.TimeConfig.class})
@ActiveProfiles("local")
public abstract class ApiTestSupport {
  static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18.4");

  static {
    DB.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", DB::getJdbcUrl);
    r.add("spring.datasource.username", DB::getUsername);
    r.add("spring.datasource.password", DB::getPassword);
    r.add("app.jwt-secret-base64", () -> "dGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXk=");
  }

  public static class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-10-10T01:00:00Z");

    public void at(Instant now) {
      this.now = now;
    }

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }

  @Autowired protected WebApplicationContext context;
  @Autowired protected ObjectMapper mapper;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected MutableClock clock;
  protected MockMvc mvc;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.execute("truncate revoked_tokens,accounts");
    clock.at(Instant.parse("2026-10-10T01:00:00Z"));
  }

  protected record Csrf(Cookie cookie, String token) {}

  protected Csrf csrf() throws Exception {
    var r = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    return new Csrf(
        r.getResponse().getCookie("XSRF-TOKEN"),
        mapper.readTree(r.getResponse().getContentAsString()).get("token").asString());
  }

  protected ResultActions send(String path, Object body, Csrf csrf, Cookie... auth)
      throws Exception {
    var req = post(path).contentType("application/json").header("X-XSRF-TOKEN", csrf.token());
    Cookie[] all = new Cookie[auth.length + 1];
    all[0] = csrf.cookie();
    System.arraycopy(auth, 0, all, 1, auth.length);
    req.cookie(all);
    if (body != null) req.content(mapper.writeValueAsString(body));
    return mvc.perform(req);
  }

  protected Map<String, String> registration(String id) {
    return Map.of("loginId", id, "password", "test-passphrase-12345", "displayName", "もち");
  }

  protected void register(String id) throws Exception {
    send("/api/accounts", registration(id), csrf()).andExpect(status().isCreated());
  }

  protected Cookie login(String id) throws Exception {
    return send(
            "/api/auth/login", Map.of("loginId", id, "password", "test-passphrase-12345"), csrf())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getCookie("auth_token");
  }
}
