package io.github.archeu.triage.ratelimit;

import io.github.archeu.triage.ai.TicketAnalysis;
import io.github.archeu.triage.ai.TicketAnalyzer;
import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Sentiment;
import io.github.archeu.triage.entity.Urgency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 限流拦截器：超限返回 429；只有反向代理转发的请求才相信 X-Real-IP。 */
@SpringBootTest(properties = "triage.rate-limit.per-ip-per-minute=2")
@AutoConfigureMockMvc
class RateLimitInterceptorTest {

    private static final String EMAIL = "{\"email\": \"Where is my order?\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketAnalyzer analyzer;

    @Test
    void returns429WithRetryAfterWhenLimitExceeded() throws Exception {
        when(analyzer.analyze(anyString(), anyString())).thenReturn(new TicketAnalysis(
                Category.INQUIRY, Urgency.LOW, Sentiment.NEUTRAL, null, "Asks about an order.", "Hi!"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(EMAIL))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(EMAIL))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("too_many_per_minute"));

        // 只限制会调用 AI 的提交，查看列表不受影响
        mvc.perform(get("/api/tickets")).andExpect(status().isOk());
    }

    @Test
    void trustsRealIpHeaderOnlyFromLocalProxy() {
        MockHttpServletRequest fromProxy = new MockHttpServletRequest();
        fromProxy.setRemoteAddr("172.18.0.1"); // Docker 网关，也就是 Caddy 转发过来的
        fromProxy.addHeader("X-Real-IP", "203.0.113.7");
        assertEquals("203.0.113.7", RateLimitInterceptor.clientIp(fromProxy));

        MockHttpServletRequest forged = new MockHttpServletRequest();
        forged.setRemoteAddr("198.51.100.20"); // 外网直接连进来，还自己带了 X-Real-IP
        forged.addHeader("X-Real-IP", "1.2.3.4");
        assertEquals("198.51.100.20", RateLimitInterceptor.clientIp(forged));
    }
}
