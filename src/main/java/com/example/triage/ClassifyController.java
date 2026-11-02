package com.example.triage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类接口：输入一封客户邮件，返回它属于哪一类。
 * 关键在提示词：讲清楚角色、类别定义、输出格式。
 * 学习用的第一版，只在本地开发（dev 配置）时启用；线上用 /api/tickets。
 */
@Profile("dev")
@RestController
@RequestMapping("/api")
public class ClassifyController {

    // 系统提示词：对应 messages 里 role=system 的那条，每次请求都会带上
    private static final String SYSTEM_PROMPT = """
            你是电商客服团队的工单分类助手。
            阅读客户邮件，判断它属于下面哪一类。只回答类别代码本身，不要任何解释或标点。

            INQUIRY   咨询：询问商品、价格、物流进度、使用方法等
            COMPLAINT 投诉：对商品质量、服务或态度表达不满，但没有要求退款
            REFUND    退款：要求退货、退款或换货
            TECHNICAL 技术问题：网站、App、支付、账号登录等出现故障
            OTHER     其他：不属于以上任何一类

            如果一封邮件同时有不满和退款要求，归为 REFUND。
            """;

    private final ChatClient chatClient;

    public ClassifyController(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @PostMapping("/classify")
    public ClassifyReply classify(@RequestBody ClassifyRequest request) {
        String raw = chatClient.prompt()
                .user(request.email())
                .call()
                .content();
        Category category = Category.parse(raw);
        // 把 AI 的原始回答也返回，方便你观察它到底说了什么
        return new ClassifyReply(category, category.label(), raw);
    }

    public record ClassifyRequest(String email) {
    }

    public record ClassifyReply(Category category, String label, String rawAnswer) {
    }
}
