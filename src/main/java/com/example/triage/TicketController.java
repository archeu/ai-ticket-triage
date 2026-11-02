package com.example.triage;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 工单接口：提交邮件 → 校验邀请码 → AI 分析 → 存库；按条件筛选列表；查看单个工单。
 * 访客隔离：每个访客只能看到自己提交的工单，外加当前语言的公开示例。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    /** 单封邮件的长度上限：防止超长内容浪费 AI 额度。 */
    private static final int MAX_EMAIL_LENGTH = 8000;
    /** 访客 ID 只接受字母、数字和横线，防止乱七八糟的输入进数据库。 */
    private static final Pattern VISITOR_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final TicketAnalyzer analyzer;
    private final TicketRepository repository;
    private final InviteService inviteService;

    public TicketController(TicketAnalyzer analyzer, TicketRepository repository, InviteService inviteService) {
        this.analyzer = analyzer;
        this.repository = repository;
        this.inviteService = inviteService;
    }

    @PostMapping
    public Ticket create(@RequestBody CreateTicketRequest request,
                         @RequestHeader(value = "X-Invite-Code", required = false) String inviteCode,
                         @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        String email = request.email();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮件内容不能为空");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("邮件太长，最多 " + MAX_EMAIL_LENGTH + " 个字符");
        }
        inviteService.consume(inviteCode); // 没有有效的邀请码就到此为止，不会调用 AI
        String summaryLanguage = "en".equalsIgnoreCase(request.summaryLanguage()) ? "English" : "中文";
        TicketAnalysis analysis = analyzer.analyze(email, summaryLanguage);
        return repository.save(Ticket.from(email, analysis, cleanVisitorId(visitorId)));
    }

    /**
     * 列表：能看到的范围是"当前语言的示例 + 自己提交的工单"，再按类别、紧急程度筛选；最新的排在前面。
     * 例：GET /api/tickets?lang=en&category=REFUND&urgency=HIGH&page=0&size=10
     */
    @GetMapping
    public TicketPage list(@RequestParam(required = false) Category category,
                           @RequestParam(required = false) Urgency urgency,
                           @RequestParam(defaultValue = "zh") String lang,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        String sampleLang = "en".equals(lang) ? "en" : "zh";
        String visitor = cleanVisitorId(visitorId);
        Specification<Ticket> filter = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            Predicate samples = cb.equal(root.get("sampleLang"), sampleLang);
            conditions.add(visitor != null ? cb.or(samples, cb.equal(root.get("visitorId"), visitor)) : samples);
            if (category != null) {
                conditions.add(cb.equal(root.get("category"), category));
            }
            if (urgency != null) {
                conditions.add(cb.equal(root.get("urgency"), urgency));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        int safeSize = Math.min(Math.max(size, 1), 100); // 每页 1–100 条，防止一次查太多
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> result = repository.findAll(filter, pageable);
        return new TicketPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** 查看单个工单：别人的工单一律返回 404，不透露它是否存在。 */
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> get(@PathVariable Long id,
                                      @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        String visitor = cleanVisitorId(visitorId);
        return repository.findById(id)
                .filter(ticket -> ticket.isVisibleTo(visitor))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static String cleanVisitorId(String visitorId) {
        return visitorId != null && VISITOR_ID.matcher(visitorId).matches() ? visitorId : null;
    }

    /** 请求不合法时返回 400 和原因。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> onBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** 没有邀请码、邀请码无效或今天用完：返回 403 和原因。 */
    @ExceptionHandler(InviteService.InviteRejectedException.class)
    public ResponseEntity<Map<String, String>> onInviteRejected(InviteService.InviteRejectedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage(), "code", e.getCode()));
    }

    /** AI 出错时返回 502 和原因，而不是一个看不懂的 500。 */
    @ExceptionHandler(TicketAnalyzer.AnalysisFailedException.class)
    public ResponseEntity<Map<String, String>> onAnalysisFailed(TicketAnalyzer.AnalysisFailedException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
    }

    /** summaryLanguage 可选："zh" 或 "en"，默认中文。 */
    public record CreateTicketRequest(String email, String summaryLanguage) {
    }

    public record TicketPage(List<Ticket> items, int page, int size, long total, int totalPages) {
    }
}
