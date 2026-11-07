package com.gamerecruitment.account.contract;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import java.util.*;
import org.junit.jupiter.api.Test;

class RegisterContractTest extends ApiTestSupport {
  @Test
  void registrationIsPublicButDoesNotLogin() throws Exception {
    var r =
        send("/api/accounts", registration("player_a"), csrf())
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn();
    var json = mapper.readTree(r.getResponse().getContentAsString());
    assertEquals(2, json.size());
    assertTrue(json.has("id"));
    assertEquals("もち", json.get("displayName").asString());
    assertNull(r.getResponse().getCookie("auth_token"));
    send("/api/accounts", registration("PLAYER_A"), csrf())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LOGIN_ID_TAKEN"));
    send("/api/accounts", registration("player_b"), csrf()).andExpect(status().isCreated());
  }

  @Test
  void invalidInputsAndUnknownFieldsAreRejected() throws Exception {
    var body = new HashMap<>(registration("aa"));
    send("/api/accounts", body, csrf())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields[0]").value("loginId"));
    body = new HashMap<>(registration("player_a"));
    body.put("displayName", "😀".repeat(30));
    send("/api/accounts", body, csrf()).andExpect(status().isCreated());
    body = new HashMap<>(registration("player_b"));
    body.put("unexpected", "value");
    send("/api/accounts", body, csrf()).andExpect(status().isBadRequest());
    var t = csrf();
    mvc.perform(
            post("/api/accounts")
                .cookie(t.cookie())
                .header("X-XSRF-TOKEN", t.token())
                .contentType("text/plain")
                .content("invalid"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void csrfAndOriginFailuresDoNotChangeData() throws Exception {
    mvc.perform(
            post("/api/accounts")
                .contentType("application/json")
                .content(mapper.writeValueAsString(registration("player_a"))))
        .andExpect(status().isForbidden());
    var t = csrf();
    mvc.perform(
            post("/api/accounts")
                .cookie(t.cookie())
                .header("X-XSRF-TOKEN", t.token())
                .header("Origin", "https://other.invalid")
                .contentType("application/json")
                .content(mapper.writeValueAsString(registration("player_a"))))
        .andExpect(status().isForbidden())
        .andExpect(header().string("Cache-Control", "no-store"));
    assertEquals(0, jdbc.queryForObject("select count(*) from accounts", Integer.class));
  }

  @Test
  void unexpectedHttpMethodsAndMultipartDoNotCauseServerErrors() throws Exception {
    var token = csrf();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/auth/csrf")
                .cookie(token.cookie())
                .header("X-XSRF-TOKEN", token.token()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    mvc.perform(get("/api/auth/csrf").contentType("multipart/form-data").content("invalid body"))
        .andExpect(status().isOk());
  }
}
