package com.gamerecruitment.recruitment.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicRecruitmentContractTest extends ApiTestSupport {
  @Test
  void paginationAndDetailExposeOnlyPublicInformation() throws Exception {
    register("public_owner");
    var owner = jdbc.queryForObject("select id from accounts", UUID.class);
    UUID id = UUID.randomUUID();
    jdbc.update("insert into recruitments (id,organizer_id,game,title,description,starts_at,capacity,meeting_info,created_at) values (?,?,?,?,?,?,?,?,?)",
        id, owner, "VALORANT", "title", "public body",
        java.sql.Timestamp.from(clock.instant().plusSeconds(3600)), 9, "private secret",
        java.sql.Timestamp.from(clock.instant()));
    mvc.perform(get("/api/recruitments/" + id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("public body"))
        .andExpect(jsonPath("$.participantCount").value(0))
        .andExpect(jsonPath("$.participants.length()").value(0))
        .andExpect(jsonPath("$.meetingInfo").doesNotExist())
        .andExpect(jsonPath("$.organizer.loginId").doesNotExist())
        .andExpect(jsonPath("$.organizer.password").doesNotExist())
        .andExpect(jsonPath("$.organizer.jwt").doesNotExist());
    for (int n = 0; n < 20; n++)
      jdbc.update("insert into recruitments (id,organizer_id,game,title,description,starts_at,capacity,created_at) values (?,?,?,?,?,?,?,?)",
          UUID.randomUUID(), owner, "VALORANT", "title", "body",
          java.sql.Timestamp.from(clock.instant().plusSeconds(3660 + n * 60)), 9,
          java.sql.Timestamp.from(clock.instant()));
    mvc.perform(get("/api/recruitments?game=VALORANT")).andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(20))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.items[0].id").value(id.toString()))
        .andExpect(jsonPath("$.items[0].meetingInfo").doesNotExist());
    mvc.perform(get("/api/recruitments?page=2")).andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  void emptyPublicResultsHaveTheContractShape() throws Exception {
    mvc.perform(get("/api/games")).andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3));
    mvc.perform(get("/api/recruitments")).andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.pageSize").value(20))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  void invalidQueriesAndUuidAreClientErrors() throws Exception {
    for (String query : new String[] {"game=UNKNOWN", "page=0", "page=-1", "page=2147483647", "page=abc"})
      mvc.perform(get("/api/recruitments?" + query)).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    mvc.perform(get("/api/recruitments/not-a-uuid")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/recruitments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
