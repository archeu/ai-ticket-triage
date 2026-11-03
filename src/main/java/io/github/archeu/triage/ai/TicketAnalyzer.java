package io.github.archeu.triage.ai;

import io.github.archeu.triage.exception.AnalysisFailedException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 工单分析：一次大模型调用，拿回类别、紧急程度、情绪、订单号、摘要和回复草稿。
 * 系统提示词放在 resources/prompts/ 里，改提示词不用改 Java 代码。
 */
@Service
public class TicketAnalyzer {

    private final ChatClient chatClient;

    public TicketAnalyzer(ChatClient.Builder builder,
                          @Value("classpath:prompts/ticket-analysis-system.st") Resource systemPrompt) {
        this.chatClient = builder.defaultSystem(systemPrompt, StandardCharsets.UTF_8).build();
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
}
