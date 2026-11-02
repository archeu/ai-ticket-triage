package com.example.triage;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * AI 对一封邮件的分析结果。
 * Spring AI 会根据这个 record 自动生成 JSON Schema 发给模型，再把模型返回的 JSON 转回这个对象。
 * 字段上的描述会写进 Schema，帮助模型理解每个字段要填什么。
 */
public record TicketAnalysis(
        @JsonPropertyDescription("工单类别")
        Category category,

        @JsonPropertyDescription("紧急程度")
        Urgency urgency,

        @JsonPropertyDescription("客户情绪")
        Sentiment sentiment,

        @JsonPropertyDescription("邮件中出现的订单号，原样提取；没有就是 null，不要编造")
        String orderNumber,

        @JsonPropertyDescription("一句话摘要，使用用户消息里指定的语言；中文不超过 40 个字，英文不超过 25 个词")
        String summary,

        @JsonPropertyDescription("给客户的回复草稿，使用客户邮件的语言")
        String replyDraft) {
}
