package io.github.archeu.triage.dto;

import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Sentiment;
import io.github.archeu.triage.entity.Ticket;
import io.github.archeu.triage.entity.Urgency;

import java.time.Instant;

/**
 * 返回给前端的工单。和数据库实体分开：实体里的访客 ID 等内部字段不会被带出去，
 * 以后改表结构也不会意外改变接口。
 *
 * @param sample 是否是公开示例（页面上显示"示例"标签）
 */
public record TicketResponse(
        Long id,
        String email,
        Category category,
        Urgency urgency,
        Sentiment sentiment,
        String orderNumber,
        String summary,
        String replyDraft,
        Instant createdAt,
        boolean sample) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getEmail(), ticket.getCategory(), ticket.getUrgency(),
                ticket.getSentiment(), ticket.getOrderNumber(), ticket.getSummary(), ticket.getReplyDraft(),
                ticket.getCreatedAt(), ticket.isSample());
    }
}
