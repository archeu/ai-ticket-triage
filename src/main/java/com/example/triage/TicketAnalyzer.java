package com.example.triage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 工单分析服务：一次调用，拿回类别、紧急程度、情绪、订单号、摘要和回复草稿。
 */
@Service
public class TicketAnalyzer {

    private static final String SYSTEM_PROMPT = """
            你是电商客服团队的工单分析助手。阅读客户邮件，输出分析结果。

            类别 category：
            - INQUIRY 咨询：询问商品、价格、物流进度、使用方法等
            - COMPLAINT 投诉：对商品质量、服务或态度表达不满，但没有要求退款
            - REFUND 退款：要求退货、退款或换货（同时有不满和退款要求时，选 REFUND）
            - TECHNICAL 技术问题：网站、App、支付、账号登录等出现故障
            - OTHER 其他：不属于以上任何一类

            紧急程度 urgency：
            - HIGH：涉及扣款异常、无法使用、强烈不满，或客户给出明确期限
            - MEDIUM：需要处理但不紧急，例如普通退换货
            - LOW：一般咨询或合作询问

            摘要 summary：使用用户消息里指定的语言。
            回复草稿 replyDraft：使用客户邮件的语言；礼貌、具体；不承诺无法确认的事，例如具体退款日期。
            """;

    private final ChatClient chatClient;

    public TicketAnalyzer(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * @param summaryLanguage 摘要用的语言，例如"中文"或"English"（跟着客服看的界面语言走）
     */
    public TicketAnalysis analyze(String email, String summaryLanguage) {
        // 用字符串拼接，不用模板占位符：邮件里如果有 { } 这类符号，模板渲染会出错
        String userMessage = "摘要 summary 使用的语言：" + summaryLanguage + "\n\n客户邮件：\n" + email;
        try {
            return chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(TicketAnalysis.class); // 结构化输出：按 Schema 要 JSON，并自动转成 Java 对象
        } catch (RuntimeException e) {
            throw new AnalysisFailedException("AI 分析失败：" + e.getMessage(), e);
        }
    }

    /** AI 调用失败或返回内容无法解析时抛出。 */
    public static class AnalysisFailedException extends RuntimeException {
        public AnalysisFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
