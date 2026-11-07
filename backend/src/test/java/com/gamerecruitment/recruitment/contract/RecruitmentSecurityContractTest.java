package com.gamerecruitment.recruitment.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.account.adapter.persistence.JdbcRevokedTokenStore;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class RecruitmentSecurityContractTest extends ApiTestSupport {
  @MockitoSpyBean JdbcRevokedTokenStore store;

  @Test
  void publicReadsIgnoreInvalidAuthenticationCookies() throws Exception {
    for (String path : new String[] {"/api/games", "/api/recruitments"}) {
      mvc.perform(get(path)).andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"));
      mvc.perform(get(path).cookie(new Cookie("auth_token", "invalid")))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/recruitments/" + UUID.randomUUID())
            .cookie(new Cookie("auth_token", "invalid")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RECRUITMENT_NOT_FOUND"));
  }

  @Test
  void privateReadsAndCsrfValidPostsRequireAuthentication() throws Exception {
    for (String path : new String[] {"/api/me/recruitments",
        "/api/recruitments/" + UUID.randomUUID() + "/meeting-info"})
      mvc.perform(get(path)).andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    send("/api/recruitments", null, csrf())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void csrfAndOriginAreCheckedBeforeAuthentication() throws Exception {
    mvc.perform(post("/api/recruitments"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(post("/api/recruitments").header("Origin", "https://untrusted.example"))
        .andExpect(status().isForbidden());
  }

  @Test
  void revocationDatabaseFailureFailsClosed() throws Exception {
    register("security_player");
    var token = login("security_player");
    doThrow(new DataAccessResourceFailureException("test failure")).when(store).isRevoked(any());
    mvc.perform(get("/api/me/recruitments").cookie(token))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
  }
}
