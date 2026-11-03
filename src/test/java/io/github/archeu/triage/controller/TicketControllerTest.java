package io.github.archeu.triage.controller;

import io.github.archeu.triage.ai.TicketAnalysis;
import io.github.archeu.triage.ai.TicketAnalyzer;
import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Invite;
import io.github.archeu.triage.entity.Sentiment;
import io.github.archeu.triage.entity.Urgency;
import io.github.archeu.triage.exception.AnalysisFailedException;
import io.github.archeu.triage.repository.InviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工单接口的集成测试：真实的 Spring 应用 + H2 内存数据库，只把 AI 换成假的（不花钱、结果固定）。
 * 覆盖邀请码、参数校验、AI 出错、访客隔离这几条线上最重要的规则。
 */
@SpringBootTest(properties = {
        "triage.invite.required=true",
        "triage.rate-limit.per-ip-per-minute=1000", // 这里不测限流，放宽上限
        "triage.rate-limit.per-ip-per-day=1000",
        "triage.rate-limit.global-per-day=1000"
})
@AutoConfigureMockMvc
class TicketControllerTest {

    private static final String INVITE = "test-invite-001";
    private static final String EMAIL = "{\"email\": \"Order #C5521 arrived broken, please refund me.\", \"summaryLanguage\": \"en\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private InviteRepository inviteRepository;

    @MockitoBean
    private TicketAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        inviteRepository.save(new Invite(INVITE, "测试", 1000));
        when(analyzer.analyze(anyString(), anyString())).thenReturn(new TicketAnalysis(
                Category.REFUND, Urgency.HIGH, Sentiment.NEGATIVE, "#C5521", "Refund request.", "Sorry about that."));
    }

    @Test
    void rejectsRequestWithoutInviteBeforeCallingAi() throws Exception {
        submit(null, newVisitor())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invite_required"));
        verify(analyzer, never()).analyze(anyString(), anyString());
    }

    @Test
    void rejectsUnknownInvite() throws Exception {
        submit("no-such-invite", newVisitor())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invite_invalid"));
    }

    @Test
    void analyzesAndSavesTicket() throws Exception {
        submit(INVITE, newVisitor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("REFUND"))
                .andExpect(jsonPath("$.urgency").value("HIGH"))
                .andExpect(jsonPath("$.orderNumber").value("C5521")) // 代码去掉了 AI 返回的 #
                .andExpect(jsonPath("$.sample").value(false))
                .andExpect(jsonPath("$.visitorId").doesNotExist()); // 内部字段不返回给前端
    }

    @Test
    void rejectsBlankEmail() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .header(ApiHeaders.INVITE_CODE, INVITE)
                        .content("{\"email\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("邮件内容不能为空"));
    }

    @Test
    void returns502WhenAiFails() throws Exception {
        when(analyzer.analyze(anyString(), anyString()))
                .thenThrow(new AnalysisFailedException("provider says: internal details", new RuntimeException()));
        submit(INVITE, newVisitor())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("analysis_failed"))
                .andExpect(jsonPath("$.error").value("AI 分析失败，请稍后再试")); // 不把内部错误透露给前端
    }

    @Test
    void visitorsOnlySeeTheirOwnTickets() throws Exception {
        String alice = newVisitor();
        String bob = newVisitor();
        String body = submit(INVITE, alice).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/tickets/" + id).header(ApiHeaders.VISITOR_ID, alice))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tickets/" + id).header(ApiHeaders.VISITOR_ID, bob))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/tickets").header(ApiHeaders.VISITOR_ID, alice))
                .andExpect(jsonPath("$.items[*].id", hasItem((int) id)));
        mvc.perform(get("/api/tickets").header(ApiHeaders.VISITOR_ID, bob))
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) id))));
    }

    @Test
    void reportsAccessStatus() throws Exception {
        mvc.perform(get("/api/access"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.valid").value(false));
        mvc.perform(get("/api/access").header(ApiHeaders.INVITE_CODE, INVITE))
                .andExpect(jsonPath("$.valid").value(true));
    }

    private ResultActions submit(String invite, String visitor) throws Exception {
        var request = post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(EMAIL)
                .header(ApiHeaders.VISITOR_ID, visitor);
        if (invite != null) {
            request.header(ApiHeaders.INVITE_CODE, invite);
        }
        return mvc.perform(request);
    }

    private static String newVisitor() {
        return UUID.randomUUID().toString();
    }
}
