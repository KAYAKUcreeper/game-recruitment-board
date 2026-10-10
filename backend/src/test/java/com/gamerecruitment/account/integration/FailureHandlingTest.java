package com.gamerecruitment.account.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.account.adapter.persistence.JdbcRevokedTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class FailureHandlingTest extends ApiTestSupport {
  @MockitoSpyBean JdbcRevokedTokenStore store;

  @Test
  void logoutFailureDoesNotClaimSuccessOrDeleteAuthCookie() throws Exception {
    register("player_a");
    var token = login("player_a");
    doThrow(new DataAccessResourceFailureException("test failure"))
        .when(store)
        .revoke(any(), any(), any());
    var response =
        send("/api/auth/logout", null, csrf(), token)
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
            .andReturn()
            .getResponse();
    assertNull(response.getCookie("auth_token"));
    mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk());
  }

  @Test
  void denylistFailureFailsClosed() throws Exception {
    register("player_a");
    var token = login("player_a");
    doThrow(new DataAccessResourceFailureException("test failure")).when(store).isRevoked(any());
    mvc.perform(get("/api/auth/me").cookie(token))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
  }
}
