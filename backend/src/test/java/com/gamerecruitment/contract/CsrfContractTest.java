package com.gamerecruitment.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.adapter.security.CsrfSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.CsrfFilter;

class CsrfContractTest {
  @Test
  void rawCookieAndHeaderAreRequired() throws Exception {
    var support = new CsrfSupport(false);
    var get = new MockHttpServletRequest("GET", "/api/auth/csrf");
    var getRes = new MockHttpServletResponse();
    var filter = new CsrfFilter(support.repository());
    filter.setRequestHandler(support);
    filter.doFilter(get, getRes, new MockFilterChain());
    String raw = support.current(get).getToken();
    assertEquals(raw, getRes.getCookie("XSRF-TOKEN").getValue());
    assertEquals("Lax", getRes.getCookie("XSRF-TOKEN").getAttribute("SameSite"));
    for (String header : new String[] {raw, "wrong", ""}) {
      var req = new MockHttpServletRequest("POST", "/api/accounts");
      req.setCookies(new Cookie("XSRF-TOKEN", raw));
      if (!header.isEmpty()) req.addHeader("X-XSRF-TOKEN", header);
      var res = new MockHttpServletResponse();
      filter.doFilter(req, res, new MockFilterChain());
      assertEquals(header.equals(raw) ? 200 : 403, res.getStatus());
    }
  }
}
