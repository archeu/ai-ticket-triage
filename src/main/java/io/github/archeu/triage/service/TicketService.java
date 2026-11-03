package io.github.archeu.triage.service;

import io.github.archeu.triage.ai.TicketAnalysis;
import io.github.archeu.triage.ai.TicketAnalyzer;
import io.github.archeu.triage.dto.CreateTicketRequest;
import io.github.archeu.triage.dto.PageResponse;
import io.github.archeu.triage.dto.TicketResponse;
import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Ticket;
import io.github.archeu.triage.entity.Urgency;
import io.github.archeu.triage.repository.TicketRepository;
import io.github.archeu.triage.repository.TicketSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 工单业务：提交 → 校验邀请码 → AI 分析 → 存库；按条件查列表；查看单个工单。
 * 访客隔离：每个访客只能看到自己提交的工单，外加当前语言的公开示例。
 */
@Service
public class TicketService {

    /** 访客 ID 只接受字母、数字和横线，防止乱七八糟的输入进数据库。 */
    private static final Pattern VISITOR_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketAnalyzer analyzer;
    private final TicketRepository repository;
    private final InviteService inviteService;

    public TicketService(TicketAnalyzer analyzer, TicketRepository repository, InviteService inviteService) {
        this.analyzer = analyzer;
        this.repository = repository;
        this.inviteService = inviteService;
    }

    /**
     * 分析并保存一封邮件。
     * 这里故意不加 @Transactional：AI 调用要好几秒，不应该一直占着数据库连接。
     */
    public TicketResponse create(CreateTicketRequest request, String inviteCode, String visitorId) {
        inviteService.consume(inviteCode); // 没有有效的邀请码就到此为止，不会调用 AI
        String summaryLanguage = "en".equalsIgnoreCase(request.summaryLanguage()) ? "English" : "中文";
        TicketAnalysis analysis = analyzer.analyze(request.email(), summaryLanguage);
        Ticket saved = repository.save(Ticket.from(request.email(), analysis, cleanVisitorId(visitorId)));
        return TicketResponse.from(saved);
    }

    /** 列表：能看到的范围是"当前语言的示例 + 自己提交的工单"，再按类别、紧急程度筛选；最新的排在前面。 */
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(Category category, Urgency urgency, String lang,
                                             int page, int size, String visitorId) {
        String sampleLang = "en".equals(lang) ? "en" : "zh";
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE); // 每页 1–100 条，防止一次查太多
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = TicketSpecifications.visibleTo(sampleLang, cleanVisitorId(visitorId), category, urgency);
        return PageResponse.from(repository.findAll(spec, pageable).map(TicketResponse::from));
    }

    /** 查看单个工单：别人的工单和不存在的工单一样返回空，不透露它是否存在。 */
    @Transactional(readOnly = true)
    public Optional<TicketResponse> findVisible(Long id, String visitorId) {
        String visitor = cleanVisitorId(visitorId);
        return repository.findById(id)
                .filter(ticket -> ticket.isVisibleTo(visitor))
                .map(TicketResponse::from);
    }

    private static String cleanVisitorId(String visitorId) {
        return visitorId != null && VISITOR_ID.matcher(visitorId).matches() ? visitorId : null;
    }
}
