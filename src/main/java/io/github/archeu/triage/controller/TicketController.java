package io.github.archeu.triage.controller;

import io.github.archeu.triage.dto.CreateTicketRequest;
import io.github.archeu.triage.dto.PageResponse;
import io.github.archeu.triage.dto.TicketResponse;
import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Urgency;
import io.github.archeu.triage.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单接口。Controller 只负责接收参数、返回结果，业务逻辑都在 TicketService 里；
 * 出错时抛异常，由 GlobalExceptionHandler 统一转成错误响应。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** 提交一封邮件：校验邀请码 → AI 分析 → 存库，返回分析结果。 */
    @PostMapping
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
                                 @RequestHeader(value = ApiHeaders.INVITE_CODE, required = false) String inviteCode,
                                 @RequestHeader(value = ApiHeaders.VISITOR_ID, required = false) String visitorId) {
        return ticketService.create(request, inviteCode, visitorId);
    }

    /** 工单列表。例：GET /api/tickets?lang=en&amp;category=REFUND&amp;urgency=HIGH&amp;page=0&amp;size=10 */
    @GetMapping
    public PageResponse<TicketResponse> list(@RequestParam(required = false) Category category,
                                             @RequestParam(required = false) Urgency urgency,
                                             @RequestParam(defaultValue = "zh") String lang,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestHeader(value = ApiHeaders.VISITOR_ID, required = false) String visitorId) {
        return ticketService.list(category, urgency, lang, page, size, visitorId);
    }

    /** 查看单个工单：别人的工单一律返回 404，不透露它是否存在。 */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> get(@PathVariable Long id,
                                              @RequestHeader(value = ApiHeaders.VISITOR_ID, required = false) String visitorId) {
        return ResponseEntity.of(ticketService.findVisible(id, visitorId));
    }
}
