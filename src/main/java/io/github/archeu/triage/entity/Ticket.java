package io.github.archeu.triage.entity;

import io.github.archeu.triage.ai.TicketAnalysis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 存进数据库的工单：原始邮件 + AI 分析结果。
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sentiment sentiment;

    @Column(length = 64)
    private String orderNumber;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String replyDraft;

    @Column(nullable = false)
    private Instant createdAt;

    /** 提交者的访客 ID（浏览器里随机生成），只用来实现"每个访客只看自己的工单"。不会返回给前端（见 dto.TicketResponse）。 */
    @Column(length = 64)
    private String visitorId;

    /** 公开示例的语言（zh / en）。普通工单为 null；示例所有访客都能看到，也不会被定时清理。 */
    @Column(length = 5)
    private String sampleLang;

    protected Ticket() {
        // JPA 需要一个无参构造方法
    }

    /** 用 AI 的分析结果创建工单。AI 漏填的字段给安全的默认值，不让脏数据进库。 */
    public static Ticket from(String email, TicketAnalysis analysis, String visitorId) {
        Ticket t = new Ticket();
        t.email = email;
        t.category = analysis.category() != null ? analysis.category() : Category.OTHER;
        t.urgency = analysis.urgency() != null ? analysis.urgency() : Urgency.MEDIUM;
        t.sentiment = analysis.sentiment() != null ? analysis.sentiment() : Sentiment.NEUTRAL;
        t.orderNumber = normalizeOrderNumber(analysis.orderNumber());
        t.summary = analysis.summary();
        t.replyDraft = analysis.replyDraft();
        t.createdAt = Instant.now();
        t.visitorId = visitorId;
        return t;
    }

    /** 订单号清洗：空值统一成 null，去掉开头的 #。确定性的格式问题交给代码，不交给 AI。 */
    private static String normalizeOrderNumber(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("null")) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1).trim() : trimmed;
    }

    /** 这个访客能不能看到这条工单：示例谁都能看，普通工单只有提交者能看。 */
    public boolean isVisibleTo(String visitorId) {
        return sampleLang != null || (visitorId != null && visitorId.equals(this.visitorId));
    }

    public boolean isSample() { return sampleLang != null; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Category getCategory() { return category; }
    public Urgency getUrgency() { return urgency; }
    public Sentiment getSentiment() { return sentiment; }
    public String getOrderNumber() { return orderNumber; }
    public String getSummary() { return summary; }
    public String getReplyDraft() { return replyDraft; }
    public Instant getCreatedAt() { return createdAt; }
}
